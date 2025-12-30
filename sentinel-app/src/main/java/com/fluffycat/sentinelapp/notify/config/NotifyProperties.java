package com.fluffycat.sentinelapp.notify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "sentinel.notify")
public record NotifyProperties(
        String defaultTo,
        String from,
        Long resendIntervalSec,
        Boolean ackResendEnabled,
        ChannelEnabled channelEnabled,
        Map<String,String> tagRoutes, //支持逗号分隔多个邮箱
        Map<String,String> envRoutes //仅支持单个邮箱
) {
    public record ChannelEnabled(Boolean email) {
    }
}