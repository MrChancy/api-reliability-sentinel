package com.fluffycat.sentinelapp.domain.dto.dashboard.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TargetOverviewItem {
    private Long targetId;
    private String name;

    private String method;
    private String baseUrl;
    private String path;

    private Boolean enabled;
    private String owner;
    private String tags;
    private LocalDateTime silencedUntil;

    // 近 5 分钟聚合
    private Long totalCnt;
    private Long failCnt;
    private BigDecimal successRatePct;   // 0~100
    private Integer p95RtMs;
    private LocalDateTime lastProbeTs;

    // 告警状态（无告警可 NONE）
    private String alertStatus;          // NONE/OPEN/ACK/RESOLVED
    private LocalDateTime alertLastSeenTs;
    private LocalDateTime alertLastSentTs;
}
