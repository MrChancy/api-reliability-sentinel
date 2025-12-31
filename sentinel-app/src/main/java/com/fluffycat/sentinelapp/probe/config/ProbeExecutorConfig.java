package com.fluffycat.sentinelapp.probe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ProbeExecutorConfig {

    @Bean
    public ThreadPoolTaskExecutor probeExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(20);
        ex.setMaxPoolSize(20);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("probe-");
        ex.initialize();
        return ex;
    }
}

