import { loadSettings, saveSettings, translate, health, whoami } from './api.js';

const $ = (id) => document.getElementById(id);
const els = {
  input: $('input'), sourceLang: $('sourceLang'), translateBtn: $('translateBtn'), micBtn: $('micBtn'),
  clearBtn: $('clearBtn'), status: $('status'), result: $('result'), translation: $('translation'),
  transliteration: $('transliteration'), meta: $('meta'), speakBtn: $('speakBtn'), copyBtn: $('copyBtn'),
  history: $('history'), settings: $('settings'), settingsBtn: $('settingsBtn'), serverUrl: $('serverUrl'),
  username: $('username'), password: $('password'), liveSelection: $('liveSelection'), autoSpeak: $('autoSpeak'),
  saveSettings: $('saveSettings'), testConn: $('testConn'), settingsMsg: $('settingsMsg')
};

let settings;
let last = null;

function setStatus(text, isError = false) {
  els.status.textContent = text;
  els.status.classList.toggle('error', isError);
}

// ---------------------------------------------------------------- translation
async function doTranslate(text = els.input.value) {
  text = (text || '').trim();
  if (!text) { setStatus('Type or select some text first.'); return; }
  els.translateBtn.disabled = true;
  setStatus('Translating…');
  try {
    last = await translate(settings, text, els.sourceLang.value);
    showResult(last);
    setStatus('');
    addHistory(last);
    if (settings.autoSpeak) speak();
  } catch (e) {
    setStatus(e.message, true);
    if (e.status === 401) els.settings.classList.remove('hidden');
  } finally {
    els.translateBtn.disabled = false;
  }
}

function showResult(r) {
  els.result.classList.remove('hidden');
  els.translation.textContent = r.translation;
  els.transliteration.textContent = r.transliteration || '';
  els.meta.textContent = `${r.sourceLanguage} → Darija · ${r.provider}/${r.model} · ${r.durationMs} ms${r.cached ? ' (cached)' : ''}`;
}

// ---------------------------------------------------------------- read aloud (text-to-speech)
function pickVoice(prefixes) {
  const voices = speechSynthesis.getVoices();
  for (const p of prefixes) {
    const v = voices.find((x) => x.lang.toLowerCase().startsWith(p));
    if (v) return v;
  }
  return null;
}

function speak() {
  if (!last) return;
  speechSynthesis.cancel();
  // Prefer a Moroccan/Arabic voice for the Arabic script; otherwise read the Latin transliteration with a French voice.
  const arabic = pickVoice(['ar-ma', 'ar']);
  let utter;
  if (arabic) {
    utter = new SpeechSynthesisUtterance(last.translation);
    utter.voice = arabic; utter.lang = arabic.lang;
  } else {
    utter = new SpeechSynthesisUtterance(last.transliteration || last.translation);
    const fr = pickVoice(['fr']);
    if (fr) { utter.voice = fr; utter.lang = fr.lang; }
    setStatus('No Arabic voice installed: reading the transliteration instead.');
  }
  utter.rate = 0.95;
  speechSynthesis.speak(utter);
}
speechSynthesis.onvoiceschanged = () => {};

// ---------------------------------------------------------------- voice input (speech-to-text -> translate -> speak)
const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
let recognizer = null;

async function ensureMicPermission() {
  try {
    const status = await navigator.permissions.query({ name: 'microphone' });
    if (status.state === 'granted') return true;
  } catch { /* permissions API unavailable */ }
  // Side panels cannot show the permission prompt, so ask once from a normal extension tab.
  await chrome.tabs.create({ url: chrome.runtime.getURL('permission.html') });
  setStatus('Allow the microphone in the new tab, then press 🎤 again.');
  return false;
}

async function toggleMic() {
  if (!Recognition) { setStatus('Speech recognition is not supported in this browser.', true); return; }
  if (recognizer) { recognizer.stop(); return; }
  if (!(await ensureMicPermission())) return;
  recognizer = new Recognition();
  const lang = { English: 'en-US', French: 'fr-FR', Spanish: 'es-ES', 'Modern Standard Arabic': 'ar-SA' }[els.sourceLang.value] || 'en-US';
  recognizer.lang = lang;
  recognizer.interimResults = true;
  recognizer.continuous = false;
  els.micBtn.classList.add('recording');
  setStatus(`Listening (${lang})…`);
  let finalText = '';
  recognizer.onresult = (e) => {
    let interim = '';
    for (const r of e.results) (r.isFinal ? (finalText = r[0].transcript) : (interim += r[0].transcript));
    els.input.value = finalText || interim;
  };
  recognizer.onerror = (e) => setStatus(`Microphone error: ${e.error}`, true);
  recognizer.onend = async () => {
    els.micBtn.classList.remove('recording');
    recognizer = null;
    if (finalText.trim()) {
      const prev = settings.autoSpeak;
      settings.autoSpeak = true; // voice in -> voice out
      await doTranslate(finalText);
      settings.autoSpeak = prev;
    } else if (!els.status.classList.contains('error')) setStatus('');
  };
  recognizer.start();
}

// ---------------------------------------------------------------- history
async function addHistory(r) {
  const { history = [] } = await chrome.storage.local.get('history');
  const next = [{ o: r.original, t: r.translation, l: r.transliteration }, ...history.filter((h) => h.o !== r.original)].slice(0, 20);
  await chrome.storage.local.set({ history: next });
  renderHistory(next);
}

function renderHistory(history) {
  els.history.replaceChildren(...history.map((h) => {
    const li = document.createElement('li');
    const o = document.createElement('span'); o.textContent = h.o;
    const t = document.createElement('span'); t.className = 'ar'; t.textContent = h.t;
    li.append(o, t);
    li.title = 'Show again';
    li.onclick = () => { els.input.value = h.o; last = { original: h.o, translation: h.t, transliteration: h.l, sourceLanguage: 'history', provider: '-', model: '-', durationMs: 0 }; showResult(last); };
    return li;
  }));
}

// ---------------------------------------------------------------- incoming text (context menu / shortcut / live selection)
async function consumePending() {
  const { pendingText } = await chrome.storage.session.get('pendingText');
  if (pendingText && pendingText.text) {
    await chrome.storage.session.remove('pendingText');
    els.input.value = pendingText.text;           // auto-paste
    if (pendingText.autoTranslate) doTranslate(pendingText.text);
  }
}

chrome.storage.onChanged.addListener((changes, area) => {
  if (area !== 'session') return;
  if (changes.pendingText && changes.pendingText.newValue) consumePending();
  if (changes.liveSelection && changes.liveSelection.newValue && settings.liveSelection) {
    els.input.value = changes.liveSelection.newValue.text; // selected text is pasted automatically
    setStatus('Selection pasted — press Translate (or Ctrl+Enter).');
  }
});

// ---------------------------------------------------------------- settings
function fillSettings() {
  els.serverUrl.value = settings.serverUrl;
  els.username.value = settings.username;
  els.password.value = settings.password;
  els.liveSelection.checked = settings.liveSelection;
  els.autoSpeak.checked = settings.autoSpeak;
}

async function onSaveSettings() {
  const url = els.serverUrl.value.trim().replace(/\/+$/, '');
  // Servers other than localhost need an optional host permission (asked at runtime).
  try {
    const origin = new URL(url).origin + '/*';
    if (!/^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?\/\*$/.test(origin)) {
      const ok = await chrome.permissions.request({ origins: [origin] });
      if (!ok) { els.settingsMsg.textContent = 'Permission to contact this server was refused.'; return; }
    }
  } catch { els.settingsMsg.textContent = 'Invalid URL'; return; }
  settings = { ...settings, serverUrl: url, username: els.username.value.trim(), password: els.password.value,
    liveSelection: els.liveSelection.checked, autoSpeak: els.autoSpeak.checked };
  await saveSettings(settings);
  els.settingsMsg.textContent = 'Saved ✓';
}

async function onTestConnection() {
  els.settingsMsg.textContent = 'Testing…';
  const draft = { ...settings, serverUrl: els.serverUrl.value.trim(), username: els.username.value.trim(), password: els.password.value };
  try {
    const h = await health(draft);
    const me = await whoami(draft);
    els.settingsMsg.textContent = `✓ ${h.service} is ${h.status} (${h.provider}/${h.model}) — logged in as ${me.user}`;
  } catch (e) {
    els.settingsMsg.textContent = '✗ ' + e.message;
  }
}

// ---------------------------------------------------------------- wiring
els.translateBtn.onclick = () => doTranslate();
els.input.addEventListener('keydown', (e) => { if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) doTranslate(); });
els.clearBtn.onclick = () => { els.input.value = ''; els.result.classList.add('hidden'); last = null; setStatus(''); };
els.speakBtn.onclick = speak;
els.copyBtn.onclick = async () => { if (last) { await navigator.clipboard.writeText(last.translation); setStatus('Copied ✓'); } };
els.micBtn.onclick = toggleMic;
els.settingsBtn.onclick = () => els.settings.classList.toggle('hidden');
els.saveSettings.onclick = onSaveSettings;
els.testConn.onclick = onTestConnection;

(async function init() {
  settings = await loadSettings();
  fillSettings();
  const { history = [] } = await chrome.storage.local.get('history');
  renderHistory(history);
  await consumePending();
})();
