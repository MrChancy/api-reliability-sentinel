package com.fluffycat.sentinelapp.domain.entity.notify;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("notify_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("alert_id")
    private Long alertId;

    @TableField("channel")
    private String channel;   // EMAIL

    @TableField("receiver")
    private String receiver;  // email

    @TableField("status")
    private String status;    // SENT / FAIL

    @TableField("error_msg")
    private String errorMsg;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
