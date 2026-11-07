package iuh.fit.demoprj.ai.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.embedding.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiConnectionProperties;
import org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingConnectionProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "google-genai")
public class AiConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(Client.class)
    Client googleGenAiClient(GoogleGenAiConnectionProperties properties,
                             @Value("${homegear.ai.request-timeout-ms:30000}") int timeoutMs) {
        Assert.hasText(properties.getApiKey(), "Configure AI_API_KEY to enable Gemini.");
        Assert.isTrue(timeoutMs > 0, "AI request timeout must be greater than zero.");
        return Client.builder()
                .apiKey(properties.getApiKey())
                .httpOptions(HttpOptions.builder().timeout(timeoutMs).build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(GoogleGenAiEmbeddingConnectionDetails.class)
    GoogleGenAiEmbeddingConnectionDetails googleGenAiEmbeddingConnectionDetails(
            GoogleGenAiEmbeddingConnectionProperties properties, Client googleGenAiClient) {
        Assert.hasText(properties.getApiKey(), "Configure AI_API_KEY for Gemini embeddings.");
        return GoogleGenAiEmbeddingConnectionDetails.builder()
                .apiKey(properties.getApiKey())
                .genAiClient(googleGenAiClient)
                .build();
    }

    @Bean("storeChatClient")
    ChatClient storeChatClient(ChatClient.Builder builder,
                               @Qualifier("storeSystemPrompt") Resource systemPrompt) {
        return builder.clone()
                .defaultSystem(systemPrompt)
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .temperature(0.3)
                        .maxOutputTokens(1024))
                .build();
    }

    @Bean("adminChatClient")
    ChatClient adminChatClient(ChatClient.Builder builder,
                               @Qualifier("adminSystemPrompt") Resource systemPrompt) {
        return builder.clone()
                .defaultSystem(systemPrompt)
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .temperature(0.2)
                        .maxOutputTokens(1200))
                .build();
    }

    @Bean("storeSystemPrompt")
    Resource storeSystemPrompt(ResourceLoader resourceLoader) {
        return resourceLoader.getResource("classpath:prompts/store-system.txt");
    }

    @Bean("adminSystemPrompt")
    Resource adminSystemPrompt(ResourceLoader resourceLoader) {
        return resourceLoader.getResource("classpath:prompts/admin-system.txt");
    }
}
