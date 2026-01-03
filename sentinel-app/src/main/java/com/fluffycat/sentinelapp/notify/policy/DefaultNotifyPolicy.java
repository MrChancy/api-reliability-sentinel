package com.fluffycat.sentinelapp.notify.policy;

import com.fluffycat.sentinelapp.common.constants.ConstantText;
import com.fluffycat.sentinelapp.common.constants.DbValues;
import com.fluffycat.sentinelapp.common.metrics.SentinelMetrics;
import com.fluffycat.sentinelapp.notify.config.NotifyProperties;
import com.fluffycat.sentinelcommon.service.NotifyPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

import static com.fluffycat.sentinelapp.common.constants.ConstantText.*;

@Component
@RequiredArgsConstructor
public class DefaultNotifyPolicy implements NotifyPolicy {

    private final NotifyProperties notifyProperties;
    private final SentinelMetrics metrics;

    @Override
    public NotifyPolicy.NotifyDecision decide(NotifyContext ctx) {

        if (ctx.silencedUntil() != null && ctx.now().isBefore(ctx.silencedUntil())){
            metrics.incNotifyThrottled("TARGET_SILENCED");
            return NotifyPolicy.NotifyDecision.no(ConstantText.SILENCED);
        }

        if (DbValues.AlertStatus.RESOLVED.equals(ctx.alertStatus()))
            return NotifyPolicy.NotifyDecision.no(ConstantText.RESOLVED_SKIP);

        if (DbValues.AlertStatus.ACK.equals(ctx.alertStatus())) {
            if (notifyProperties.ackResendEnabled() == null || !notifyProperties.ackResendEnabled()) {
                return NotifyPolicy.NotifyDecision.no(ConstantText.ACK_SKIP);
            }
        }

        return switch (ctx.upsertAction()) {
            case OPEN_CREATED -> NotifyPolicy.NotifyDecision.yes(OPEN_CREATED);
            case REOPENED -> NotifyPolicy.NotifyDecision.yes(REOPENED);
            case OPEN_UPDATED -> throttle(ctx);
            default -> NotifyPolicy.NotifyDecision.no(UNKNOWN_ACTION + ": " + ctx.upsertAction());
        };

    }

    private NotifyPolicy.NotifyDecision throttle(NotifyContext ctx) {
        long intervalSec = Optional.ofNullable(notifyProperties.resendIntervalSec()).orElse(600L);

        if (ctx.lastSentTs() == null)
            return NotifyPolicy.NotifyDecision.yes(ConstantText.FIRST_SEND);

        long gapSec = Duration.between(ctx.lastSentTs(), ctx.now()).getSeconds();
        if (gapSec >= intervalSec) {
            return NotifyPolicy.NotifyDecision.yes(ConstantText.THROTTLE_PASS);
        }
        metrics.incNotifyThrottled("DB_THROTTLE");
        return NotifyPolicy.NotifyDecision.no(ConstantText.THROTTLE + "(" + gapSec + "/" + "(" + intervalSec + "s)");
    }
}
