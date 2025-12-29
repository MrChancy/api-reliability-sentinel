package com.fluffycat.sentinelcommon.service;

public interface NotificationPort {
    void enqueue(AlertNotificationCommand cmd);

    record AlertNotificationCommand(
            long alertId,
            long targetId,
            String dedupeKey,
            String action,      // OPEN_CREATED / REOPENED / THROTTLE_PASS 等（来自 NotifyPolicy reason 或 action）
            String summary,
            String detailsJson,
            String env,
            java.time.LocalDateTime occurredAt
    ) {}

}
