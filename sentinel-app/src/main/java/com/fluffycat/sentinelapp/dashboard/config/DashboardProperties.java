package com.fluffycat.sentinelapp.dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sentinel.dashboard")
public record DashboardProperties (
        int targetWindowMinutes
){
}
