package com.fluffycat.sentinelapp.common.pagination;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sentinel.pagination")
public record PaginationProperties(
        Integer defaultSize,
        Integer maxSize
) {
    public int defaultSizeOr(int fallback) {
        return (defaultSize == null || defaultSize <= 0) ? fallback : defaultSize;
    }
    public int maxSizeOr(int fallback) {
        return (maxSize == null || maxSize <= 0) ? fallback : maxSize;
    }
}