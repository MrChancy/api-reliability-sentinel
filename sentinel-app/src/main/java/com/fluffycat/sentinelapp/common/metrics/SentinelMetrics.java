package com.fluffycat.sentinelapp.common.metrics;

import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class SentinelMetrics {

    private final MeterRegistry registry;
    private final ConcurrentHashMap<String,Timer> probeRtTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<CounterKey,Counter> probeTotalCounter = new ConcurrentHashMap<>();

    // ---------- Probe ----------
    public void recordProbe(String env,
                            boolean success,
                            String errorType,
                            Integer httpCode,
                            int rtMs) {

        String result = success ? "SUCCESS" : "FAIL";
        String et = (errorType == null || errorType.isBlank()) ? "UNKNOWN" : errorType;
        String hc = (httpCode == null) ? "NA" : String.valueOf(httpCode);

        CounterKey key = new CounterKey(env,result,et,hc);

        Counter counter  = probeTotalCounter.computeIfAbsent(key,k->
                     Counter.builder("sentinel_probe_total")
                            .description("Probe attempts total")
                            .tags("env", k.env(),
                                    "result", k.result(),
                                    "error_type", k.errorType(),
                                    "http_code", k.httpCode())
                            .register(registry)

        );

        counter.increment();

        Timer timer = probeRtTimers.computeIfAbsent(env,k->
            Timer.builder("sentinel_probe_rt_ms")
                    .description("Probe latency in milliseconds")
                    .tags("env", k)
                    .publishPercentileHistogram(true)
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(registry)

        );
        timer.record(rtMs, TimeUnit.MILLISECONDS);
    }

    // ---------- Alert ----------
    public void incAlertUpsert(String alertType, String action) {
        Counter.builder("sentinel_alert_upsert_total")
                .description("Alert upsert actions total")
                .tags("alert_type", safe(alertType),
                        "action", safe(action))
                .register(registry)
                .increment();
    }

    public void incAlertResolved(String alertType) {
        Counter.builder("sentinel_alert_resolve_total")
                .description("Alert resolved total")
                .tags("alert_type", safe(alertType))
                .register(registry)
                .increment();
    }

    // ---------- Notify ----------
    public void incNotifySend(String channel, boolean success) {
        Counter.builder("sentinel_notify_send_total")
                .description("Notification send total")
                .tags("channel", safe(channel),
                        "result", success ? "SENT" : "FAIL")
                .register(registry)
                .increment();
    }

    public void incNotifyThrottled(String reason) {
        Counter.builder("sentinel_notify_throttled_total")
                .description("Notification throttled/dropped total")
                .tags("reason", safe(reason))
                .register(registry)
                .increment();
    }

    private String safe(String v) {
        return (v == null || v.isBlank()) ? "NA" : v.trim();
    }

    private record CounterKey(String env, String result, String errorType, String httpCode) {}
}
