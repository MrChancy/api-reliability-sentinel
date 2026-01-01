package com.fluffycat.sentinelapp.domain.dto.dashboard.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TimeseriesPoint {
    private LocalDateTime bucketTs;

    private Long totalCnt;
    private Long failCnt;
    private BigDecimal errorRatePct;

    private Integer avgRtMs;
    private Integer p95RtMs;
}
