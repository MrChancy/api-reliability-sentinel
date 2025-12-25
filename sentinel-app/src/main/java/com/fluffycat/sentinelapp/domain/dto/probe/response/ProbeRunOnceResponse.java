package com.fluffycat.sentinelapp.domain.dto.probe.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProbeRunOnceResponse {
    private Long targetId;
    private int times;
    private int successCount;
    private int failureCount;
    private long avgLatencyMs;
    private ProbeRunOnceLast last;

    @Data
    @Builder
    public static class ProbeRunOnceLast {
        private boolean success;
        private Integer httpStatus;
        private Long latencyMs;
        private String errorMessage;
    }
}
