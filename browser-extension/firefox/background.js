let panelPort = null;

chrome.runtime.onConnect.addListener((port) => {
  if (port.name === "dataspex-panel") {
    panelPort = port;

    port.onDisconnect.addListener(() => {
      panelPort = null;
    });
  }
});

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.from === "dataspex-library" && panelPort) {
    panelPort.postMessage(message);
  }
});
