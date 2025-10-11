function injectContentScript() {
  chrome.runtime.sendMessage({
    type: "inject-script",
    tabId: chrome.devtools.inspectedWindow.tabId
  });
}

chrome.devtools.panels.create("Dataspex", "dataspex-48.png", "panel.html", function(panel) {
  injectContentScript();
  chrome.devtools.network.onNavigated.addListener(injectContentScript);
});
