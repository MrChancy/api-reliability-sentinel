package com.fluffycat.sentinelapp.domain.dto.dashboard.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AlertOverviewItem {
    private Long alertId;
    private Long targetId;

    private String targetName;
    private String method;
    private String baseUrl;
    private String path;

    private String alertType;           // ERROR_RATE
    private String alertLevel;          // P1
    private String status;              // OPEN/ACK/RESOLVED

    private LocalDateTime firstSeenTs;
    private LocalDateTime lastSeenTs;
    private LocalDateTime lastSentTs;

    private Integer countInWindow;
    private String summary;

    private String owner;
    private LocalDateTime silencedUntil;
}
