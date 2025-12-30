package com.fluffycat.sentinelapp.domain.entity.alert;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("alert_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEventEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("target_id")
    private Long targetId;


    @TableField("alert_type")
    private String alertType; // ERROR_RATE

    /** 告警类型：ERROR_RATE / CONSEC_FAIL / TIMEOUT_SPIKE 等 */
    @TableField("alert_level")
    private String alertLevel; // P1

    @TableField("dedupe_key")
    private String dedupeKey;

    @TableField("status")
    private String status; // OPEN / ACK / RESOLVED

    @TableField("first_seen_ts")
    private LocalDateTime firstSeenTs;

    @TableField("last_seen_ts")
    private LocalDateTime lastSeenTs;

    @TableField("last_sent_ts")
    private LocalDateTime lastSentTs;

    /** 同一告警周期内触发次数累计（演示/统计用） */
        @TableField("count_in_window")
    private Integer countInWindow;

    /** 一句话摘要（邮件标题/列表展示） */
    @TableField("summary")
    private String summary;

    /** 详情 JSON（窗口、阈值、失败样本等） */
    @TableField("details_json")
    private String detailsJson; // MySQL JSON，用 String 存合法 JSON

    /** 创建时间/更新时间：审计用途 */
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
