// Reports text the user selects on the page so the side panel can paste it automatically.
(() => {
  let last = '';
  let timer = null;
  const report = () => {
    const text = String(window.getSelection() || '').trim();
    if (text && text !== last && text.length <= 5000) {
      last = text;
      try {
        chrome.runtime.sendMessage({ type: 'selection', text });
      } catch (e) {
        /* extension reloaded: ignore */
      }
    }
  };
  document.addEventListener('mouseup', () => { clearTimeout(timer); timer = setTimeout(report, 250); });
  document.addEventListener('keyup', (e) => { if (e.shiftKey || e.key === 'Shift') { clearTimeout(timer); timer = setTimeout(report, 400); } });
})();
