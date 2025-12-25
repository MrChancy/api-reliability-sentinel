package com.fluffycat.sentinelapp.domain.dto.probe.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProbeRunOnceRequest {

    @NotNull
    private Long targetId;

    /** 演示 burst：默认 1；建议上限 50，防止一次请求跑太久 */
    @Min(1) @Max(50)
    private Integer times = 1;

    /** 两次探测间隔，演示建议 200ms；默认 0 */
    @Min(0) @Max(10_000)
    private Integer intervalMs = 0;
}
