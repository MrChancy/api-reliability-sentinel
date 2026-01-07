package com.fluffycat.sentinelapp.probe.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;

@Configuration
public class ProbeExecutorConfig {

    @Bean
    public ThreadPoolTaskExecutor probeExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(20);
        ex.setMaxPoolSize(20);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("probe-");
        ex.setTaskDecorator(new MdcTaskDecorator());
        ex.initialize();
        return ex;
    }

    public static class MdcTaskDecorator implements TaskDecorator{

        @Override
        public Runnable decorate(Runnable runnable) {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return ()->{
                Map<String, String> previous = MDC.getCopyOfContextMap();
                try{
                    if (contextMap!=null)
                        MDC.setContextMap(contextMap);
                    runnable.run();
                }finally {
                    if (previous!=null)
                        MDC.setContextMap(previous);
                    else
                        MDC.clear();
                }
            };

        }
    }
}

