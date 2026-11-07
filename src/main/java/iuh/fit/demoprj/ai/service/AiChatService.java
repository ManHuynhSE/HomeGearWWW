package iuh.fit.demoprj.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {
    private final ObjectProvider<ChatClient> storeChatClient;
    private final ObjectProvider<ChatClient> adminChatClient;

    public AiChatService(@Qualifier("storeChatClient") ObjectProvider<ChatClient> storeChatClient,
                         @Qualifier("adminChatClient") ObjectProvider<ChatClient> adminChatClient) {
        this.storeChatClient = storeChatClient;
        this.adminChatClient = adminChatClient;
    }

    public String answerStoreQuestion(String message) {
        return answer(storeChatClient.getIfAvailable(), message);
    }

    public String answerAdminQuestion(String message) {
        return answer(adminChatClient.getIfAvailable(), message);
    }

    private String answer(ChatClient chatClient, String message) {
        if (chatClient == null) {
            throw new AiServiceUnavailableException();
        }
        try {
            String reply = chatClient.prompt().user(message).call().content();
            if (reply == null || reply.isBlank()) {
                throw new IllegalStateException("AI provider returned an empty response.");
            }
            return reply;
        } catch (RuntimeException exception) {
            throw AiProviderException.from(exception);
        }
    }
}
