package com.fluffycat.sentinelapp.common.pagination;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.function.Function;

@Data
@Builder
public class PageResponse<T> {
    private List<T> records;
    private long total;
    private long page;
    private long size;
    private long pages;

    public static <E, T> PageResponse<T> from(Page<E> p, Function<E, T> mapper) {
        long size = p.getSize();
        long total = p.getTotal();
        return PageResponse.<T>builder()
                .records(p.getRecords().stream().map(mapper).toList())
                .total(total)
                .page(p.getCurrent())
                .size(size)
                .pages(calcPages(total, size))
                .build();
    }

    public static <T> PageResponse<T> unpaged(List<T> records) {
        long total = records == null ? 0 : records.size();
        return PageResponse.<T>builder()
                .records(records)
                .total(total)
                .page(1)
                .size(0)
                .pages(total > 0 ? 1 : 0)
                .build();
    }

    private static long calcPages(long total, long size) {
        if (size <= 0) return 0;
        return (total + size - 1) / size;
    }
}
