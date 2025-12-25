package com.fluffycat.sentinelapp.domain.dto.target.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TargetResponse {
    private Long id;
    private String name;
    private Boolean enabled;
    private String method;
    private String baseUrl;
    private String path;
    private Integer intervalSec;
    private Integer timeoutMs;
    private Integer windowSec;
    private BigDecimal errorRateThreshold;
    private Integer failThreshold;
    private LocalDateTime silencedUntil;
    private String silenceReason;
    private Integer retries;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Schema(description = "环境：local/docker", example = "docker")
    private String env;

}
