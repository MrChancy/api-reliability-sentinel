package com.fluffycat.sentinelapp.alert.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sentinel.alert")
public record AlertProperties(
    int jobIntervalSec,
    int sampleCount
) {
}
