const port = chrome.runtime.connect({ name: "dataspex-panel" });

port.onMessage.addListener((message) => {
  console.log("Panel received message:", message);
  sendToPage("HELLO!!!");
});

function sendToPage(message) {
  const serialized = JSON.stringify({
    from: "dataspex-extension",
    payload: JSON.stringify(message)
  });

  chrome.devtools.inspectedWindow.eval(
    `window.postMessage(${serialized}, "*")`,
    (result, isException) => {
      if (isException) {
        console.error("Failed to send message to page:", isException);
      }
    }
  );
}
