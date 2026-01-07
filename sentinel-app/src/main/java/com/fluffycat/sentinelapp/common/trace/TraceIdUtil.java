package com.fluffycat.sentinelapp.common.trace;

import org.slf4j.MDC;

import java.util.UUID;

public class TraceIdUtil {
    private TraceIdUtil() {
    }

    public static final String TRACE_ID = "traceId";
    public static final String SCAN_ID = "scanId";
    public static final String TARGET_ID = "targetId";
    public static final String ALERT_ID = "alertId";

    public static String getOrCreate(String key) {
        String v = MDC.get(key);
        if (v == null || v.isBlank()) {
            v = newUniqueId();
            MDC.put(key, v);
        }
        return v;
    }

    public static String newUniqueId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void clear() {
        if (MDC.get(TRACE_ID) != null) MDC.remove(TRACE_ID);
        if (MDC.get(SCAN_ID) != null) MDC.remove(SCAN_ID);
    }
}
