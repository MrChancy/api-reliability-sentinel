package com.fluffycat.sentinelapp.alert.job;

import com.fluffycat.sentinelapp.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertJob {
    private final AlertService alertService;

    @Scheduled(fixedDelay = 5000)
    private void run() {
        int n = alertService.scanErrorRateAndUpsert();
        if (n > 0) {
            log.info("AlertJob generated/updated {} alert_event", n);
        }
    }
}
