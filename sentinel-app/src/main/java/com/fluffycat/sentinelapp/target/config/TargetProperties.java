package com.fluffycat.sentinelapp.target.config;

import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "sentinel.target")
public record TargetProperties (
        // 允许为空：为空时走 profile 推断
        @Pattern(regexp = "^(|local|docker)$", message = "sentinel.target.defaultEnv must be local or docker")
        String defaultEnv)
{}
