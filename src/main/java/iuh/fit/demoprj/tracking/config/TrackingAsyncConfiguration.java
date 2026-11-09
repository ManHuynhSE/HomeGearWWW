package iuh.fit.demoprj.tracking.config;

import java.time.Clock;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
@EnableAsync
public class TrackingAsyncConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(TrackingAsyncConfiguration.class);

    @Bean("trackingExecutor")
    ThreadPoolTaskExecutor trackingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("product-tracking-");
        executor.setRejectedExecutionHandler((task, pool) ->
                logger.warn("Product tracking queue is full; dropping the event."));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        return executor;
    }

    @Bean
    Clock trackingClock() {
        return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
