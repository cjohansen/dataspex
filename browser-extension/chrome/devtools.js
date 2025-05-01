function injectContentScript() {
  chrome.scripting.executeScript({
    target: { tabId: chrome.devtools.inspectedWindow.tabId },
    files: ["content-script.js"]
  });
}

chrome.devtools.panels.create("Dataspex", "" /* icon */, "panel.html", function (panel) {
  injectContentScript();
  chrome.devtools.network.onNavigated.addListener(injectContentScript);
});
