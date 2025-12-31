package com.fluffycat.sentinelapp.notify.listen;

import com.fluffycat.sentinelapp.alert.repo.AlertEventMapper;
import com.fluffycat.sentinelapp.domain.entity.alert.AlertEventEntity;
import com.fluffycat.sentinelapp.domain.entity.target.TargetEntity;
import com.fluffycat.sentinelapp.notify.config.NotifyProperties;
import com.fluffycat.sentinelapp.notify.lock.RedisSendLock;
import com.fluffycat.sentinelapp.notify.service.NotifyService;
import com.fluffycat.sentinelapp.target.repo.TargetMapper;
import com.fluffycat.sentinelcommon.service.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final TargetMapper targetMapper;
    private final NotifyService notifyService;
    private final AlertEventMapper alertEventMapper;
    private final NotifyProperties notifyProperties;
    private final RedisSendLock redisSendLock;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(NotificationPort.AlertNotificationCommand cmd) {

        long intervalSec = Optional.ofNullable(notifyProperties.resendIntervalSec())
                .filter(interval -> interval > 0)
                .orElse(600L);

        boolean acquired = redisSendLock.tryAcquire(cmd.dedupeKey(), Duration.ofSeconds(intervalSec));
        if (!acquired) {
            log.info("Send suppressed by redis lock, alertId={}, dedupekey={}", cmd.alertId(), cmd.dedupeKey());
            return;
        }

        TargetEntity target = targetMapper.selectById(cmd.targetId());
        if (target == null) {
            log.warn("target not found, skip notify: targetId={}, alertId={}", cmd.targetId(), cmd.alertId());
            return;
        }

        String subject = "[API Sentinel] " + cmd.summary();
        String body = buildBody(cmd, target);

        LocalDateTime now = LocalDateTime.now();

        try {
            notifyService.sendEmail(cmd.alertId(), target, subject, body);

            // 仅成功发送才写 last_sent_ts（用于频控）
            AlertEventEntity upd = new AlertEventEntity();
            upd.setId(cmd.alertId());
            upd.setLastSentTs(now);
            alertEventMapper.updateById(upd);

        } catch (Exception e) {
            log.warn("notify failed: alertId={}, targetId={}, reason={}",
                    cmd.alertId(), cmd.targetId(), cmd.action(), e);
        }
    }

    private String buildBody(NotificationPort.AlertNotificationCommand cmd, TargetEntity target) {
        return """
                %s
                
                Target: %s %s%s
                Env: %s
                Action: %s
                
                Details:
                %s
                """.formatted(
                cmd.summary(),
                target.getMethod(),
                target.getBaseUrl(),
                target.getPath(),
                cmd.env(),
                cmd.action(),
                cmd.detailsJson()
        );
    }
}