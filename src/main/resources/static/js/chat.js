(() => {
  const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content;

  document.querySelectorAll("[data-chat]").forEach(chat => {
    const endpoint = chat.dataset.endpoint;
    const messages = chat.querySelector("[data-chat-messages]");
    const form = chat.querySelector("[data-chat-form]");
    const input = form?.querySelector("[name='message']");
    if (!endpoint || !messages || !(form instanceof HTMLFormElement) || !(input instanceof HTMLInputElement || input instanceof HTMLTextAreaElement)) return;

    const addMessage = (text, sender, retryMessage = null) => {
      const item = document.createElement("article");
      item.className = `hg-chat-message hg-chat-message-${sender}`;
      const body = document.createElement("p");
      body.className = "hg-chat-message-body";
      body.textContent = text;
      item.append(body);

      if (retryMessage) {
        const retry = document.createElement("button");
        retry.type = "button";
        retry.className = "hg-chat-retry";
        retry.textContent = "Thử gửi lại";
        retry.addEventListener("click", () => sendMessage(retryMessage));
        item.append(retry);
      }

      messages.append(item);
      messages.scrollTop = messages.scrollHeight;
      return item;
    };

    const setWelcome = () => {
      messages.replaceChildren();
      addMessage(chat.dataset.endpoint === "/api/admin/assistant"
        ? "Xin chào! Tôi là trợ lý quản trị HomeGear. Bạn có thể đặt câu hỏi để thử nghiệm."
        : "Xin chào! Tôi có thể hỗ trợ bạn tìm hiểu sản phẩm và dịch vụ HomeGear.", "assistant");
    };

    const newConversationButtons = [...chat.querySelectorAll("[data-chat-new]")];
    let resettingConversation = false;
    const startNewConversation = async () => {
      if (form.dataset.pending === "true" || resettingConversation) return;
      resettingConversation = true;
      newConversationButtons.forEach(button => { button.disabled = true; });
      input.disabled = true;
      const sendButton = form.querySelector("button[type='submit']");
      if (sendButton) sendButton.disabled = true;
      try {
        const headers = {};
        if (csrfToken) headers["X-CSRF-TOKEN"] = csrfToken;
        const response = await fetch(`${endpoint}/conversation`, {
          method: "POST",
          headers,
          credentials: "same-origin"
        });
        if (response.status !== 204) throw new Error("Không thể bắt đầu cuộc trò chuyện mới. Vui lòng thử lại.");
        setWelcome();
      } catch (error) {
        addMessage(error instanceof Error ? error.message : "Đã xảy ra lỗi. Vui lòng thử lại.", "error");
      } finally {
        resettingConversation = false;
        newConversationButtons.forEach(button => { button.disabled = false; });
        input.disabled = false;
        if (sendButton) sendButton.disabled = false;
      }
    };

    const sendMessage = async message => {
      const normalizedMessage = message.trim();
      if (!normalizedMessage || form.dataset.pending === "true" || resettingConversation) return;

      form.dataset.pending = "true";
      const sendButton = form.querySelector("button[type='submit']");
      if (sendButton) sendButton.disabled = true;
      input.disabled = true;
      addMessage(normalizedMessage, "user");
      const loading = addMessage("Đang trả lời...", "assistant");

      try {
        const headers = { "Content-Type": "application/json" };
        if (csrfToken) headers["X-CSRF-TOKEN"] = csrfToken;
        const response = await fetch(endpoint, {
          method: "POST",
          headers,
          credentials: "same-origin",
          body: JSON.stringify({ message: normalizedMessage })
        });
        const result = await response.json().catch(() => ({}));
        loading.remove();
        if (!response.ok) throw new Error(result.error || "Không thể gửi câu hỏi. Vui lòng thử lại.");
        if (typeof result.reply !== "string" || !result.reply.trim()) throw new Error("Trợ lý chưa gửi được câu trả lời. Vui lòng thử lại.");
        addMessage(result.reply, "assistant");
      } catch (error) {
        loading.remove();
        addMessage(error instanceof Error ? error.message : "Đã xảy ra lỗi. Vui lòng thử lại.", "error", normalizedMessage);
      } finally {
        form.dataset.pending = "false";
        if (sendButton) sendButton.disabled = false;
        input.disabled = false;
        input.focus();
      }
    };

    form.addEventListener("submit", event => {
      event.preventDefault();
      const message = input.value;
      if (!message.trim()) return;
      input.value = "";
      sendMessage(message);
    });

    chat.querySelectorAll("[data-chat-suggestions] button").forEach(button => {
      button.addEventListener("click", () => {
        input.value = button.textContent.trim();
        form.requestSubmit();
      });
    });

    newConversationButtons.forEach(button => {
      button.addEventListener("click", startNewConversation);
    });

    const panel = chat.querySelector(".hg-chat-panel");
    const launcher = chat.querySelector(".hg-chat-launcher");
    const closeButton = chat.querySelector("[data-chat-close]");
    if (panel && launcher) {
      const setOpen = open => {
        panel.hidden = !open;
        launcher.setAttribute("aria-expanded", String(open));
        if (open) input.focus();
      };
      launcher.addEventListener("click", () => setOpen(panel.hidden));
      closeButton?.addEventListener("click", () => setOpen(false));
    }

    setWelcome();
  });
})();
