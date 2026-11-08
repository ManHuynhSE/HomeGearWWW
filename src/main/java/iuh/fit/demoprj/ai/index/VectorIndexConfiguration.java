package iuh.fit.demoprj.ai.index;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
@EnableAsync
@ConditionalOnProperty(name = "spring.ai.vectorstore.type", havingValue = "mariadb")
public class VectorIndexConfiguration {

    @Bean("aiReindexExecutor")
    ThreadPoolTaskExecutor aiReindexExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("ai-vector-reindex-");
        return executor;
    }
}
