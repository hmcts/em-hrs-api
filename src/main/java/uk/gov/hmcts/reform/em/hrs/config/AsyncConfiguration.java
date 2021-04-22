package uk.gov.hmcts.reform.em.hrs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AsyncConfiguration {
    @Value("${hrs.thread-pool.core-size}")
    private int coreSize;

    @Value("${hrs.thread-pool.max-size}")
    private int maxSize;

    @Value("${hrs.thread-pool.queue-capacity}")
    private int queueCapacity;

    @Bean(name = "HrsAsyncExecutor")
    public Executor provideAsyncExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("HrsAsyncThread-");
        executor.initialize();
        return executor;
    }
}
