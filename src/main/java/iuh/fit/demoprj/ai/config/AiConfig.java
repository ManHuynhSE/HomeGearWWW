package iuh.fit.demoprj.ai.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.embedding.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiConnectionProperties;
import org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingConnectionProperties;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import iuh.fit.demoprj.ai.tools.AdminTools;
import iuh.fit.demoprj.ai.tools.SemanticProductTools;
import iuh.fit.demoprj.ai.tools.StoreTools;
import org.springframework.beans.factory.ObjectProvider;
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

    @Bean
    ToolCallingAdvisor toolCallingAdvisor() {
        ToolCallingManager manager = ToolCallingManager.builder()
                .maxCallsPerTool(3)
                .maxTotalToolCalls(8)
                .build();
        return ToolCallingAdvisor.builder()
                .toolCallingManager(manager)
                .build();
    }

    @Bean
    MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ai.vectorstore.type", havingValue = "mariadb")
    QuestionAnswerAdvisor policyQuestionAnswerAdvisor(
            @Qualifier("vectorStore") VectorStore vectorStore) {
        FilterExpressionBuilder filter = new FilterExpressionBuilder();
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(4)
                .similarityThreshold(0.65)
                .filterExpression(filter.eq("type", "policy").build())
                .build();
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();
    }

    @Bean("storeChatClient")
    ChatClient storeChatClient(ChatClient.Builder builder,
                               @Qualifier("storeSystemPrompt") Resource systemPrompt,
                               ToolCallingAdvisor toolCallingAdvisor,
                               MessageChatMemoryAdvisor memoryAdvisor,
                               StoreTools storeTools,
                               ObjectProvider<QuestionAnswerAdvisor> policyAdvisorProvider,
                               ObjectProvider<SemanticProductTools> semanticToolsProvider) {
        ChatClient.Builder chatClientBuilder = builder.clone().defaultSystem(systemPrompt);
        QuestionAnswerAdvisor policyAdvisor = policyAdvisorProvider.getIfAvailable();
        if (policyAdvisor == null) {
            chatClientBuilder.defaultAdvisors(toolCallingAdvisor, memoryAdvisor);
        } else {
            chatClientBuilder.defaultAdvisors(toolCallingAdvisor, policyAdvisor, memoryAdvisor);
        }
        SemanticProductTools semanticTools = semanticToolsProvider.getIfAvailable();
        Object[] tools = semanticTools == null
                ? new Object[] {storeTools}
                : new Object[] {storeTools, semanticTools};
        return chatClientBuilder
                .defaultTools(tools)
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .temperature(0.3)
                        .maxOutputTokens(1024))
                .build();
    }

    @Bean("adminChatClient")
    ChatClient adminChatClient(ChatClient.Builder builder,
                               @Qualifier("adminSystemPrompt") Resource systemPrompt,
                               ToolCallingAdvisor toolCallingAdvisor,
                               MessageChatMemoryAdvisor memoryAdvisor,
                               AdminTools adminTools) {
        return builder.clone()
                .defaultSystem(systemPrompt)
                .defaultAdvisors(toolCallingAdvisor, memoryAdvisor)
                .defaultTools(adminTools)
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
