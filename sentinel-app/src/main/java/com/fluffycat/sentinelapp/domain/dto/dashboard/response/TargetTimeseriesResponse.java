package com.fluffycat.sentinelapp.domain.dto.dashboard.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TargetTimeseriesResponse {
    private String env;
    private Long targetId;

    private Integer rangeMinutes;       // 固定 60
    private Integer bucketSec;          // 固定 60
    private LocalDateTime generatedAt;

    private List<TimeseriesPoint> points;
    private List<ErrorTypeBreakdownItem> errorTypeBreakdown;
}
