package com.fluffycat.sentinelapp.domain.dto.dashboard.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorTypeBreakdownItem {
    private String errorType;       // TIMEOUT/DNS/CONN/5XX/4XX/UNKNOWN...
    private Long count;
}
