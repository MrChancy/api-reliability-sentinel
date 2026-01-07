package com.fluffycat.sentinelapp.common.trace;

import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

public final class MdcScope implements AutoCloseable{
    private final Map<String,String> previous;

    private MdcScope(Map<String,String> previous){
        this.previous = previous;
    }

    public static MdcScope of (Map<String,String> entries){
        Map<String, String> prev = MDC.getCopyOfContextMap();
        if (entries != null) {
            entries.forEach((k, v) -> {
                if (v == null) MDC.remove(k);
                else MDC.put(k, v);
            });
        }
        return new MdcScope(prev);
    }

    public static MdcScope put(String key, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        return of(map);
    }

    @Override
    public void close() {
        if (previous == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(previous);
        }
    }
}
