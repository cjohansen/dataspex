function injectContentScript() {
  chrome.runtime.sendMessage({
    type: "inject-script",
    tabId: chrome.devtools.inspectedWindow.tabId
  });
}

chrome.devtools.panels.create("Dataspex", "", "panel.html", function(panel) {
  injectContentScript();
  chrome.devtools.network.onNavigated.addListener(injectContentScript);
});
