package com.fluffycat.sentinelapp.domain.entity.probe;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("probe_event")
public class ProbeEventEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("target_id")
    private Long targetId;

    /** 探测发生时间 */
    @TableField("ts")
    private LocalDateTime ts;

    /** SUCCESS / FAIL */
    @TableField("status")
    private String status;

    @TableField("http_code")
    private Integer httpCode;

    @TableField("rt_ms")
    private Integer rtMs;

    /** TIMEOUT / DNS / CONN / 5XX / 4XX / UNKNOWN */
    @TableField("error_type")
    private String errorType;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("trace_id")
    private String traceId;

    @TableField("sample")
    private String sample;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
