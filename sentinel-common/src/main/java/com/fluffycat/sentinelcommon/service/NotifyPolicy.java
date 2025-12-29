package com.fluffycat.sentinelcommon.service;

import java.time.LocalDateTime;

public interface NotifyPolicy {
    NotifyDecision decide(NotifyContext ctx);

    record NotifyContext(
            String upsertAction,      // OPEN_CREATED / OPEN_UPDATED / REOPENED
            String alertStatus,       // OPEN/ACK/RESOLVED
            LocalDateTime lastSentTs,
            LocalDateTime silencedUntil,
            LocalDateTime now
    ) {}

    record NotifyDecision(boolean shouldSend, String reason) {
        public static NotifyDecision yes(String reason) { return new NotifyDecision(true, reason); }
        public static NotifyDecision no(String reason)  { return new NotifyDecision(false, reason); }
    }
}
