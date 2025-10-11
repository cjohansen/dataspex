chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.from === "dataspex-library") {
    chrome.devtools.inspectedWindow.eval(
      'window.postMessage({ from: "dataspex-extension", data: "Hi from extension!" }, "*");'
    );
  }
});

chrome.devtools.inspectedWindow.eval(
  'window.postMessage({ from: "dataspex-extension", message: "{:event :extension-loaded}" }, "*");'
);
