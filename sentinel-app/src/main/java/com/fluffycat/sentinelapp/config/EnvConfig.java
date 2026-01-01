package com.fluffycat.sentinelapp.config;

import com.fluffycat.sentinelapp.common.env.DefaultEnvResolver;
import com.fluffycat.sentinelapp.common.env.EnvResolver;
import com.fluffycat.sentinelapp.target.config.TargetProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvConfig {

    @Bean
    public EnvResolver envResolver(TargetProperties targetProperties){
        return new DefaultEnvResolver(targetProperties::defaultEnv);
    }
}
