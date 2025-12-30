package com.fluffycat.sentinelapp.domain.dto.alert.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AlertResponse {
    private String id;
    private String status;
    private LocalDateTime updatedAt;
}
