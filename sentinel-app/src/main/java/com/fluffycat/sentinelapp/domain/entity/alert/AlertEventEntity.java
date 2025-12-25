package com.fluffycat.sentinelapp.domain.entity.alert;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

    @TableField("count_in_window")
    private Integer countInWindow;

    @TableField("summary")
    private String summary;

    @TableField("details_json")
    private String detailsJson; // MySQL JSON，用 String 存合法 JSON
}
