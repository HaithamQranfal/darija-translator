# Demo video script (about 5 minutes)

Record with OBS or the Windows Game Bar (Win+Alt+R). Before you record: start the server, load the extension, open Postman, and have `php -S`, Python and Expo running.

| Time | Show on screen | Say |
|---|---|---|
| 0:00–0:20 | README on GitHub | "Hi, I'm Haitham Qranfal. This is Mini Project 2: an LLM-powered RESTful web service that translates text into Moroccan Darija, with a Chrome extension, a PHP app, a Python app and a React Native app as clients." |
| 0:20–0:45 | Deployment diagram, then class diagram | "The server is a Maven WAR running on Payara Micro, an embedded Jakarta EE 10 container. Every client talks to it over HTTP with JSON and Basic authentication. The service calls Gemini, or a local Ollama model." |
| 0:45–1:30 | `TranslatorResource.java` → `TranslationService` → `LlmClient` / `GeminiClient` | "TranslatorResource is the Jakarta REST endpoint. Its translate() method takes the text and delegates to TranslationService, which builds the prompt, checks the cache and calls an LlmClient. LlmClient is a strategy interface, so I can switch between Gemini, Ollama and a mock with one environment variable. Gemini returns JSON with the Darija in Arabic script plus a transliteration." |
| 1:30–2:10 | `BasicAuthServerAuthModule.java`, `AuthModuleRegistrar.java`, `web.xml` | "Security uses the Jakarta Authentication specification. This ServerAuthModule decodes the Basic header, checks salted SHA-256 hashes, and gives the caller and its group to the container through callbacks. It is registered at start-up with AuthConfigFactory.registerServerAuthModule, and web.xml protects /api/translator/*." |
| 2:10–2:40 | Terminal: `mvn clean package payara-micro:start`, then Postman | Run Health (public), then Translate JSON, No credentials → 401, Wrong password → 401, Empty text → 400. "These are the Postman tests: a valid call, and the security and validation errors." |
| 2:40–3:40 | Chrome on a news article | Select a sentence, right-click, choose **Translate to Darija**: the side panel opens and translates it. Select another sentence: it is pasted automatically. Click 🔊 Read aloud, then 🎤 and say "Where is the nearest pharmacy?" for voice-to-voice. Open ⚙ and click Test connection. |
| 3:40–4:05 | PHP page at localhost:8000, then `php cli.php "..."` | "The PHP client uses cURL with Basic auth." |
| 4:05–4:25 | `python translator_client.py -i`, then `gui_app.py` | "The Python client uses only the standard library, with a CLI and a Tkinter GUI." |
| 4:25–4:50 | Phone mirrored with scrcpy, or Expo web | Translate a sentence, then Read aloud. "The React Native app runs in Expo Go and reaches the server over the LAN." |
| 4:50–5:00 | GitHub commit history | "All the code, the UML and the Postman collection are on GitHub. Thank you!" |

Good sentences to translate: "I'm running late, I'll be there in ten minutes.", "How much does this cost?", "Bonjour, je cherche la gare.", "Thank you very much, my friend!"
