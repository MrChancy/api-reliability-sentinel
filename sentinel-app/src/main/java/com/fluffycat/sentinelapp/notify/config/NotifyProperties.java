package com.fluffycat.sentinelapp.notify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sentinel.notify")
public record NotifyProperties(
        String defaultTo,
        String from,
        Long resendIntervalSec,
        Boolean ackResendEnabled,
        ChannelEnabled channelEnabled
) {
    public record ChannelEnabled(Boolean email) {
    }
}