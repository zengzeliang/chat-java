package com.brother.chatgpt.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ConfigurationProperties(prefix = "threadpool")
@Data
public class ThreadPoolConfig {
    private Integer corePoolSize;
    private Integer maxPoolSize;
    private Integer queueCapacity;
    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("MessageTokenLenCompute-");
        executor.initialize();
        return executor;
    }
}
