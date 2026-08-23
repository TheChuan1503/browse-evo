browser.runtime.onMessage.addListener((message, sender, sendResponse) => {
  browser.runtime.sendNativeMessage("evo", message).then(
    (response) => sendResponse(response),
    () => sendResponse(null)
  );
  return true;
});
