const panelPortsByTabId = new Map();
const panelPortPrefix = "dataspex-panel-"

chrome.runtime.onConnect.addListener((port) => {
  if (port.name.startsWith(panelPortPrefix)) {
    const tabId = parseInt(port.name.slice(panelPortPrefix.length));
    tabId && panelPortsByTabId.set(tabId, port);

    port.onDisconnect.addListener(() => {
      tabId && panelPortsByTabId.delete(tabId);
    });
  }
});

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.from === "dataspex-library") {
    const tabId = sender.tab?.id;
    const port = panelPortsByTabId.get(tabId);
    port?.postMessage(message);
  }
});
