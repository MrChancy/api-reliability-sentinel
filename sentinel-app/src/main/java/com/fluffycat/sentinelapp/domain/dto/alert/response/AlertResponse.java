package com.fluffycat.sentinelapp.domain.dto.alert.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AlertResponse {
    private Long id;
    private Long targetId;
    private String status;
    private LocalDateTime firstSeenTs;

    private LocalDateTime lastSeenTs;

    private LocalDateTime lastSentTs;
    private LocalDateTime updatedAt;
    /**
     * 一句话摘要（邮件标题/列表展示）
     */
    private String summary;
}
