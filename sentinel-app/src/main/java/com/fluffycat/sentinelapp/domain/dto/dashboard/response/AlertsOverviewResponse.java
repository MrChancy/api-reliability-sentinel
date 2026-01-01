package com.fluffycat.sentinelapp.domain.dto.dashboard.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AlertsOverviewResponse {
    private String env;
    private String status;              // OPEN/ACK/RESOLVED
    private LocalDateTime generatedAt;
    private List<AlertOverviewItem> items;
}
