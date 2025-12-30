package com.fluffycat.sentinelapp.target.converter;

import com.fluffycat.sentinelapp.domain.dto.target.request.CreateTargetRequest;
import com.fluffycat.sentinelapp.domain.dto.target.request.UpdateTargetRequest;
import com.fluffycat.sentinelapp.domain.dto.target.response.TargetResponse;
import com.fluffycat.sentinelapp.domain.entity.target.TargetEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public final class TargetConverter {

    private TargetConverter() {
    }

    // ====== Request -> Entity ======

    public static TargetEntity toEntity(CreateTargetRequest req, String env) {
        // 默认值策略（M1）
        Integer intervalSec = defaultInt(req.getIntervalSec(), 60);
        Integer timeoutMs = defaultInt(req.getTimeoutMs(), 800);
        Integer retries = defaultInt(req.getRetries(), 0);
        Integer windowSec = defaultInt(req.getWindowSec(), 300);
        BigDecimal threshold = defaultDouble(req.getErrorRateThreshold(), BigDecimal.valueOf(50.0));
        LocalDateTime silenceUntil = Optional.ofNullable(req.getSilenceMinutes())
                .map(LocalDateTime.now()::plusMinutes)
                .orElse(null);



        return TargetEntity.builder()
                .name(trim(req.getName()))
                .enabled(req.getEnabled() != null ? req.getEnabled() : Boolean.TRUE)
                .method(defaultStr(req.getMethod(), "GET"))
                .baseUrl(trim(req.getBaseUrl()))
                .path(normalizePath(req.getPath()))
                .intervalSec(intervalSec)
                .timeoutMs(timeoutMs)
                .retries(retries)
                .windowSec(windowSec)
                .errorRateThreshold(threshold)
                .silencedUntil(silenceUntil)
                .silenceReason(req.getSilenceReason())
                .tags(trim(req.getTags()))
                .owner(trim(req.getOwner()))
                .env(env)
                .build();
    }

    /**
     * Update 通常是“合并更新”：把 req 非空字段覆盖到 existing
     */
    public static void merge(UpdateTargetRequest req, TargetEntity existing) {
        if (req.getName() != null) existing.setName(trim(req.getName()));
        if (req.getEnabled() != null) existing.setEnabled(req.getEnabled());
        if (req.getMethod() != null) existing.setMethod(req.getMethod());
        if (req.getBaseUrl() != null) existing.setBaseUrl(trim(req.getBaseUrl()));
        if (req.getPath() != null) existing.setPath(normalizePath(req.getPath()));
        if (req.getSilenceMinutes() != null) {
            LocalDateTime silenceUntil = LocalDateTime.now().plusMinutes(req.getSilenceMinutes());
            existing.setSilencedUntil(silenceUntil);
        }
        if (req.getSilenceReason()!=null) existing.setSilenceReason(req.getSilenceReason());

        if (req.getIntervalSec() != null) existing.setIntervalSec(req.getIntervalSec());
        if (req.getTimeoutMs() != null) existing.setTimeoutMs(req.getTimeoutMs());
        if (req.getRetries() != null) existing.setRetries(req.getRetries());
        if (req.getWindowSec() != null) existing.setWindowSec(req.getWindowSec());
        if (req.getErrorRateThreshold() != null) existing.setErrorRateThreshold(req.getErrorRateThreshold());

        if (req.getTags() != null) existing.setTags(trim(req.getTags()));
        if (req.getOwner() != null) existing.setOwner(trim(req.getOwner()));
        if (req.getEnv() != null) existing.setEnv(req.getEnv());

        // 如果 updated_at 交给 DB on update，这里不必 set；否则：
        // existing.setUpdatedAt(LocalDateTime.now());
    }

    // ====== Entity -> Response ======

    public static TargetResponse toResponse(TargetEntity e) {
        if (e == null) return null;
        return TargetResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .enabled(e.getEnabled())
                .method(e.getMethod())
                .baseUrl(e.getBaseUrl())
                .path(e.getPath())
                .intervalSec(e.getIntervalSec())
                .timeoutMs(e.getTimeoutMs())
                .retries(e.getRetries())
                .windowSec(e.getWindowSec())
                .errorRateThreshold(e.getErrorRateThreshold())
                .silencedUntil(e.getSilencedUntil())
                .silenceReason(e.getSilenceReason())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .env(e.getEnv())
                .build();
    }

    // ====== helpers ======

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static String defaultStr(String s, String def) {
        String t = trim(s);
        return (t == null || t.isEmpty()) ? def : t;
    }

    private static Integer defaultInt(Integer v, int def) {
        return v == null ? def : v;
    }

    private static BigDecimal defaultDouble(BigDecimal v, BigDecimal def) {
        return v == null ? def : v;
    }

    private static String normalizePath(String path) {
        String p = trim(path);
        if (p == null || p.isEmpty()) return "/";
        return p.startsWith("/") ? p : "/" + p;
    }
}
