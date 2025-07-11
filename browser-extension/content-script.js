// This script is injected into the inspected page

window.addEventListener("message", function(event) {
  if (event.source !== window) {
    return;
  }

  if (event.data.from === "dataspex-library") {
    // Relay messages from the inspected page to the extension
    chrome.runtime.sendMessage(event.data);
    return;
  }
});

window.postMessage({from: "dataspex-content-script", type: "ready"}, "*");
