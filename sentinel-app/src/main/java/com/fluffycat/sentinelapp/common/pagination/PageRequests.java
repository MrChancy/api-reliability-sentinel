package com.fluffycat.sentinelapp.common.pagination;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PageRequests {

    private final PaginationProperties props;

    public PageRequest of(Integer page, Integer size) {
        int defaultSize = props.defaultSizeOr(20);
        int maxSize = props.maxSizeOr(100);
        return PageRequest.of(page, size, defaultSize, maxSize);
    }

    // 特例：允许覆盖（少数场景用）
    public PageRequest of(Integer page, Integer size, int defaultSize, int maxSize) {
        return PageRequest.of(page, size, defaultSize, maxSize);
    }
}
