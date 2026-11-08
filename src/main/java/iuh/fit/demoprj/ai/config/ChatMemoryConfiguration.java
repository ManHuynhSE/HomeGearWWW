package iuh.fit.demoprj.ai.config;

import iuh.fit.demoprj.ai.web.ConversationIdResolver;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ChatMemoryConfiguration {

    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    @Bean
    ServletListenerRegistrationBean<HttpSessionListener> guestChatMemoryCleanup(ChatMemory chatMemory) {
        HttpSessionListener listener = new HttpSessionListener() {
            @Override
            public void sessionDestroyed(HttpSessionEvent event) {
                chatMemory.clear(ConversationIdResolver.guestStoreConversationId(event.getSession().getId()));
            }
        };
        return new ServletListenerRegistrationBean<>(listener);
    }
}
