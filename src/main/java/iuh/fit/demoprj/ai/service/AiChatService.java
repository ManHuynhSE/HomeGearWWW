package iuh.fit.demoprj.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {
    private final ObjectProvider<ChatClient> storeChatClient;
    private final ObjectProvider<ChatClient> adminChatClient;
    private final ChatMemory chatMemory;

    public AiChatService(@Qualifier("storeChatClient") ObjectProvider<ChatClient> storeChatClient,
                         @Qualifier("adminChatClient") ObjectProvider<ChatClient> adminChatClient,
                         ChatMemory chatMemory) {
        this.storeChatClient = storeChatClient;
        this.adminChatClient = adminChatClient;
        this.chatMemory = chatMemory;
    }

    public String answerStoreQuestion(String message, String conversationId) {
        return answer(storeChatClient.getIfAvailable(), message, conversationId);
    }

    public String answerAdminQuestion(String message, String conversationId) {
        return answer(adminChatClient.getIfAvailable(), message, conversationId);
    }

    public void clearConversation(String conversationId) {
        chatMemory.clear(conversationId);
    }

    private String answer(ChatClient chatClient, String message, String conversationId) {
        if (chatClient == null) {
            throw new AiServiceUnavailableException();
        }
        try {
            String reply = chatClient.prompt()
                    .user(message)
                    .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();
            if (reply == null || reply.isBlank()) {
                throw new IllegalStateException("AI provider returned an empty response.");
            }
            return reply;
        } catch (RuntimeException exception) {
            throw AiProviderException.from(exception);
        }
    }
}
