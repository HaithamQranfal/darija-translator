# Building a Darija translator: Jakarta REST, Jakarta Authentication, Gemini, and four clients

*By Haitham Qranfal, Computer Science student at Al Akhawayn University*

Moroccan Darija is spoken by more than 30 million people, but most translation tools only know Modern Standard Arabic. For a Web Services course project I built a small system that puts a Large Language Model behind a secure Java REST API, then calls it from a Chrome extension, a PHP site, a Python app and a React Native mobile app. This post walks through how it is built and what went wrong along the way.

## Architecture

The core is a Maven WAR built on the Jakarta EE 10 APIs and run on **Payara Micro**, an embedded container you start with a single command. The `TranslatorResource` class exposes one business method, `translate()`, at `POST /api/translator/translate`. Behind it:

* `TranslationService` validates the input, looks in a small LRU cache, and builds the prompt.
* `LlmClient` is a strategy interface with three implementations: `GeminiClient` (Google's free tier), `OllamaClient` (a 3B-parameter model running locally) and `MockLlmClient` (for offline demos). A CDI producer chooses one from an environment variable.

## Prompting for *real* Darija

My first prompts came back in Standard Arabic: grammatical, but nobody in Casablanca talks like that. What fixed it:

1. Ask explicitly for Darija as spoken in Casablanca and Rabat, and list example words (`bghit`, `daba`, `bzaf`, `kifach`).
2. Ask for a **JSON** answer containing both Arabic script and a Latin transliteration ("Arabizi"). Gemini's `responseMimeType: application/json` makes parsing reliable, and the transliteration helps learners and text-to-speech engines.
3. Put the user's text between `<<<TEXT … TEXT>>>` markers and tell the model to treat it only as content. Without that, a selected paragraph saying "ignore previous instructions…" would actually be obeyed.

## Security with Jakarta Authentication

Most tutorials protect a REST API with a JAX-RS filter. The assignment asked for the **Jakarta Authentication** specification (the former JASPIC), so I wrote a `ServerAuthModule`. Its `validateRequest()` method:

* decodes `Authorization: Basic …`;
* checks the password against salted SHA-256 hashes, compared in constant time;
* reports the caller and its `translator` group to the container through `CallerPrincipalCallback` and `GroupPrincipalCallback`;
* or answers `401` with a `WWW-Authenticate` challenge.

Since Jakarta Authentication 3.0, registering the module takes one line in a `ServletContextListener`: `AuthConfigFactory.getFactory().registerServerAuthModule(module, servletContext)`. The usual `web.xml` security constraint decides which URLs require authentication, so `/api/health` stays public.

A detail worth handling: a `401` carrying `WWW-Authenticate: Basic` can make the browser open its own login pop-up. My clients now send an `X-Requested-With` header, and for those requests the module answers with a non-standard scheme name, so the browser leaves the error to the app.

## The Chrome extension (Manifest V3)

* `chrome.contextMenus` adds **Translate "…" to Darija** to the right-click menu.
* `chrome.sidePanel.open()` must be called *synchronously* inside the user gesture. If any `await` comes before it, Chrome refuses to open the panel. I start the open call first, then save the selected text to `chrome.storage.session`, and the panel picks the text up when it loads.
* A small content script reports new selections, so text is pasted into the open panel automatically.
* **Voice-to-voice** chains the Web Speech API (`webkitSpeechRecognition`), the REST call, and `speechSynthesis`. Side panels can't show the microphone permission prompt, so the extension opens a one-off tab to ask for it.

## The other clients

* **PHP:** cURL with `CURLAUTH_BASIC`, a form plus a CLI script.
* **Python:** standard library only (`urllib`), with an argparse CLI and a Tkinter GUI.
* **React Native (Expo SDK 58):** `fetch` with a UTF-8-safe Base64 encoder, `expo-speech` for reading aloud, and AsyncStorage for settings. The classic trap: `localhost` on a phone is the phone itself, so the app's settings take the PC's LAN IP.

## Lessons learned

* Hide the LLM behind an interface. If Gemini's free-tier quota runs out, switching to a local Ollama model is a one-line config change.
* Cache repeated phrases. Demos repeat the same sentences a lot.
* Write the UML from the code, not before it, so the class diagram matches what actually ships.

Source code: *(GitHub link)*
