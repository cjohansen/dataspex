chrome.devtools.panels.create("Dataspex", "" /* icon */, "panel.html", function (panel) {
  chrome.scripting.executeScript({
    target: { tabId: chrome.devtools.inspectedWindow.tabId },
    files: ["content-script.js"]
  });
});
