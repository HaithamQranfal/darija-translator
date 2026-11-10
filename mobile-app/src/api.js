// Client for the TranslatorResource REST endpoint (HTTP Basic authentication).

// On a physical phone "localhost" is the phone itself: use your PC's LAN IP (e.g. http://192.168.1.20:8080/translator).
// Android emulator: http://10.0.2.2:8080/translator
export const DEFAULT_SETTINGS = {
  serverUrl: 'http://192.168.1.20:8080/translator',
  username: 'haitham',
  password: 'darija2026',
};

const B64 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

/** UTF-8 safe Base64 (does not rely on a global btoa). */
export function base64(str) {
  const bytes = unescape(encodeURIComponent(str));
  let out = '';
  for (let i = 0; i < bytes.length; i += 3) {
    const a = bytes.charCodeAt(i);
    const b = i + 1 < bytes.length ? bytes.charCodeAt(i + 1) : NaN;
    const c = i + 2 < bytes.length ? bytes.charCodeAt(i + 2) : NaN;
    const n = (a << 16) | ((b || 0) << 8) | (c || 0);
    out += B64[(n >> 18) & 63] + B64[(n >> 12) & 63] + (isNaN(b) ? '=' : B64[(n >> 6) & 63]) + (isNaN(c) ? '=' : B64[n & 63]);
  }
  return out;
}

async function call(settings, path, options = {}) {
  const url = settings.serverUrl.replace(/\/+$/, '') + path;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 60000);
  try {
    const res = await fetch(url, {
      ...options,
      signal: controller.signal,
      headers: {
        Accept: 'application/json',
        'X-Requested-With': 'DarijaTranslatorMobile',
        Authorization: 'Basic ' + base64(`${settings.username}:${settings.password}`),
        ...(options.headers || {}),
      },
    });
    let body = null;
    try { body = await res.json(); } catch { /* not JSON */ }
    if (!res.ok) {
      throw new Error(res.status === 401 ? 'Wrong username or password (401)' : `${res.status}: ${(body && body.message) || 'Server error'}`);
    }
    return body;
  } catch (e) {
    if (e.name === 'AbortError') throw new Error('The server took too long to answer');
    if (e.message && e.message.includes('Network request failed')) {
      throw new Error(`Cannot reach ${settings.serverUrl}. Is the server running and is the IP correct?`);
    }
    throw e;
  } finally {
    clearTimeout(timer);
  }
}

export const translate = (settings, text, sourceLanguage = 'auto') =>
  call(settings, '/api/translator/translate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text, sourceLanguage }),
  });

export const health = (settings) => call(settings, '/api/health');
export const whoami = (settings) => call(settings, '/api/translator/whoami');
