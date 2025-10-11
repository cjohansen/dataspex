function injectContentScript() {
  chrome.scripting.executeScript({
    target: { tabId: chrome.devtools.inspectedWindow.tabId },
    files: ["content-script.js"]
  });
}

chrome.devtools.panels.create("Dataspex", "dataspex-48.png", "panel.html", function (panel) {
  panel.onShown.addListener(function (panelWindow) {
    panelWindow.chrome_devtools = chrome.devtools;
  });

  injectContentScript();
  chrome.devtools.network.onNavigated.addListener(injectContentScript);
});
