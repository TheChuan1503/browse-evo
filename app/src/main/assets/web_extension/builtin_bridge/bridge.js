(function () {
  try {
    if (typeof browser === "undefined" || typeof browser.runtime === "undefined") {
      return;
    }
    window.addEventListener("evo:searchOrOpen", function (event) {
      try {
        browser.runtime.sendMessage({
          action: "searchOrOpen",
          value: String(event.detail)
        });
      } catch (e) {
        // ignore
      }
    });
    window.wrappedJSObject.evo = {
      searchOrOpen: function (value) {
        window.dispatchEvent(
          new CustomEvent("evo:searchOrOpen", { detail: String(value) })
        );
      }
    };
  } catch (e) {
    // ignore
  }
})();
