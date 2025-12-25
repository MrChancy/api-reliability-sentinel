package com.fluffycat.sentinelapp.domain.entity.target;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@TableName("probe_target")
public class TargetEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Boolean enabled;
    private String method;
    private String baseUrl;
    private String path;
    private String headersJson;
    private String bodyJson;
    private Integer intervalSec;
    private Integer timeoutMs;
    private Integer retries;
    private Integer windowSec;
    private BigDecimal errorRateThreshold;
    private Integer failThreshold;
    private LocalDateTime silencedUntil;
    private String silenceReason;
    private String tags;
    private String owner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String env;
}