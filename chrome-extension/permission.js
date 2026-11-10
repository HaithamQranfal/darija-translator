// Side panels cannot display the microphone prompt, so the extension asks once from this tab.
navigator.mediaDevices.getUserMedia({ audio: true })
  .then((stream) => {
    stream.getTracks().forEach((t) => t.stop());
    document.getElementById('msg').textContent = 'Microphone allowed ✓ — you can close this tab and use 🎤 in the side panel.';
    setTimeout(() => window.close(), 1500);
  })
  .catch((e) => {
    document.getElementById('msg').textContent = 'Microphone blocked (' + e.message + '). Allow it from the address-bar icon and reload.';
  });
