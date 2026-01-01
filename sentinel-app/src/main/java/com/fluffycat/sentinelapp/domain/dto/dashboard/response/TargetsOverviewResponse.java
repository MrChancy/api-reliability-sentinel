package com.fluffycat.sentinelapp.domain.dto.dashboard.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TargetsOverviewResponse {
    private String env;
    private Integer windowMinutes;       // 固定 5
    private LocalDateTime generatedAt;
    private List<TargetOverviewItem> items;
}
