(() => {
  const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content;
  const csrfHeaderName = document.querySelector('meta[name="csrf-header-name"]')?.content;
  const validSources = new Set(["HOME", "LISTING", "SEARCH", "CHATBOT", "DETAIL"]);

  document.addEventListener("click", event => {
    if (!(event.target instanceof Element)) return;
    const target = event.target.closest('[data-track="PRODUCT_CLICK"]');
    if (!(target instanceof HTMLElement)) return;

    const productId = Number(target.dataset.productId);
    const source = target.dataset.source;
    if (!Number.isSafeInteger(productId) || productId <= 0
        || !validSources.has(source) || !csrfToken || !csrfHeaderName) {
      return;
    }

    fetch("/api/track", {
      method: "POST",
      keepalive: true,
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/json",
        [csrfHeaderName]: csrfToken
      },
      body: JSON.stringify({ event: "PRODUCT_CLICK", productId, source })
    }).catch(() => {});
  });
})();
