package com.fluffycat.sentinelapp.probe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sentinel.probe")
public record ProbeProperties(
        String env,
        int batchSize,
        int leaseSec,
        long tickMs) {
}
