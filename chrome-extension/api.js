// Small client for the TranslatorResource REST endpoint (shared by the side panel).
export const DEFAULT_SETTINGS = {
  serverUrl: 'http://localhost:8080/translator',
  username: 'haitham',
  password: 'darija2026',
  liveSelection: true,
  autoSpeak: false,
  speechLang: 'en-US'
};

export async function loadSettings() {
  const { settings } = await chrome.storage.local.get('settings');
  return { ...DEFAULT_SETTINGS, ...(settings || {}) };
}

export async function saveSettings(settings) {
  await chrome.storage.local.set({ settings });
}

function basicAuth(user, pass) {
  // btoa only handles Latin-1: encode UTF-8 first.
  const bytes = new TextEncoder().encode(`${user}:${pass}`);
  let bin = '';
  bytes.forEach((b) => (bin += String.fromCharCode(b)));
  return 'Basic ' + btoa(bin);
}

function base(settings) {
  return settings.serverUrl.replace(/\/+$/, '');
}

async function call(settings, path, options = {}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 60000);
  try {
    const res = await fetch(base(settings) + path, {
      ...options,
      signal: controller.signal,
      headers: {
        Accept: 'application/json',
        'X-Requested-With': 'DarijaTranslatorExtension',
        Authorization: basicAuth(settings.username, settings.password),
        ...(options.headers || {})
      }
    });
    let body = null;
    try { body = await res.json(); } catch { /* not JSON */ }
    if (!res.ok) {
      const msg = (body && body.message) || res.statusText;
      const err = new Error(res.status === 401 ? `Wrong username or password (401)` : `${res.status}: ${msg}`);
      err.status = res.status;
      throw err;
    }
    return body;
  } catch (e) {
    if (e.name === 'AbortError') throw new Error('The server took too long to answer');
    if (e instanceof TypeError) throw new Error(`Cannot reach ${base(settings)} — is the server running?`);
    throw e;
  } finally {
    clearTimeout(timeout);
  }
}

/** Calls the translate() business method. */
export function translate(settings, text, sourceLanguage = 'auto') {
  return call(settings, '/api/translator/translate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text, sourceLanguage })
  });
}

export function health(settings) {
  return call(settings, '/api/health');
}

export function whoami(settings) {
  return call(settings, '/api/translator/whoami');
}
