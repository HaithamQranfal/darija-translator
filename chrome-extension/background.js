// Service worker: context menu, keyboard shortcut and hand-off of the selected text to the side panel.
const MENU_TRANSLATE = 'darija-translate-selection';
const MENU_OPEN = 'darija-open-panel';

chrome.runtime.onInstalled.addListener(() => {
  chrome.contextMenus.removeAll(() => {
    chrome.contextMenus.create({
      id: MENU_TRANSLATE,
      title: 'Translate "%s" to Darija',
      contexts: ['selection']
    });
    chrome.contextMenus.create({
      id: MENU_OPEN,
      title: 'Open Darija Translator',
      contexts: ['page', 'frame', 'link', 'image']
    });
  });
  // Clicking the toolbar icon also opens the side panel.
  chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true }).catch(console.error);
});

/** Stores the text for the side panel (which may not be open yet) and opens the panel. */
async function sendToPanel(text, tab) {
  // sidePanel.open() must be called synchronously inside the user gesture, before any await.
  const opening = chrome.sidePanel.open({ windowId: tab.windowId });
  if (text && text.trim()) {
    await chrome.storage.session.set({ pendingText: { text: text.trim(), at: Date.now(), autoTranslate: true } });
  }
  await opening.catch((e) => console.warn('sidePanel.open failed', e));
}

chrome.contextMenus.onClicked.addListener((info, tab) => {
  if (!tab) return;
  if (info.menuItemId === MENU_TRANSLATE) sendToPanel(info.selectionText || '', tab);
  if (info.menuItemId === MENU_OPEN) sendToPanel('', tab);
});

chrome.commands.onCommand.addListener((command, tab) => {
  if (command !== 'translate-selection' || !tab) return;
  const opening = chrome.sidePanel.open({ windowId: tab.windowId });
  chrome.scripting
    .executeScript({ target: { tabId: tab.id }, func: () => String(window.getSelection() || '') })
    .then(async ([res]) => {
      if (res && res.result) {
        await chrome.storage.session.set({ pendingText: { text: res.result.trim(), at: Date.now(), autoTranslate: true } });
      }
    })
    .catch((e) => console.warn('Cannot read selection on this page', e));
  opening.catch((e) => console.warn(e));
});

// Live selection coming from content.js: the panel auto-pastes it when "Live selection" is on.
chrome.runtime.onMessage.addListener((msg) => {
  if (msg && msg.type === 'selection' && msg.text) {
    chrome.storage.session.set({ liveSelection: { text: msg.text, at: Date.now() } });
  }
});
