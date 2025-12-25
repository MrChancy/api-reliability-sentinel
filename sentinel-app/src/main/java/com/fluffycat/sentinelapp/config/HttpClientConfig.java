package com.fluffycat.sentinelapp.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class HttpClientConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(500))
                .readTimeout(Duration.ofMillis(1500))
                .writeTimeout(Duration.ofMillis(500))
                .retryOnConnectionFailure(false)
                .build();
    }
}
