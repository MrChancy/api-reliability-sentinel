package com.fluffycat.sentinelapp.common.pagination;

import lombok.Getter;

@Getter
public class PageRequest {
    private final long page;      // 1-based
    private final long size;      // 0 表示 all
    private final long offset;    // size=0 时 offset=0
    private final boolean unpaged;

    private PageRequest(long page, long size) {
        this.unpaged = (size == 0);
        this.page = unpaged ? 1 : page;
        this.size = size;
        this.offset = unpaged ? 0 : (page - 1) * size;
    }

    public static PageRequest of(Integer page, Integer size, long defaultSize, long maxSize) {
        long s;
        if (size == null) {
            s = defaultSize;                 // 默认分页
        } else if (size == 0) {
            s = 0;                           // unpaged
        } else {
            s = Math.min(Math.max(size, 1), maxSize);
        }

        long p = (page == null) ? 1 : Math.max(page, 1);
        return new PageRequest(p, s);
    }
}
