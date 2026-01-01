package com.fluffycat.sentinelapp.alert.job;

import com.fluffycat.sentinelapp.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertJob {
    private final AlertService alertService;

    @Value("${sentinel.enable-job:true}")
    private Boolean enableJob;

    @Scheduled(fixedDelay = 5000)
    private void run() {
        if (!enableJob) return;

        int n = alertService.scanErrorRateAndUpsert();
        if (n > 0) {
            log.info("AlertJob generated/updated {} alert_event", n);
        }
    }
}
