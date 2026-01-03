package com.fluffycat.sentinelapp.probe.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluffycat.sentinelapp.common.api.ErrorCode;
import com.fluffycat.sentinelapp.common.exception.BusinessException;
import com.fluffycat.sentinelapp.common.metrics.SentinelMetrics;
import com.fluffycat.sentinelapp.domain.dto.probe.request.ProbeRunOnceRequest;
import com.fluffycat.sentinelapp.domain.dto.probe.response.ProbeRunOnceResponse;
import com.fluffycat.sentinelapp.domain.entity.probe.ProbeEventEntity;
import com.fluffycat.sentinelapp.domain.entity.target.TargetEntity;
import com.fluffycat.sentinelapp.probe.repo.ProbeEventMapper;
import com.fluffycat.sentinelapp.target.repo.TargetMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ProbeRunnerService {

    private final TargetMapper targetMapper;
    private final ProbeEventMapper probeEventMapper;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final SentinelMetrics metrics;

    public ProbeRunOnceResponse runOnce(ProbeRunOnceRequest req) {
        TargetEntity target = targetMapper.selectById(req.getTargetId());
        if (target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "target not found: " + req.getTargetId());
        }

        int times = req.getTimes() == null ? 1 : req.getTimes();
        int intervalMs = req.getIntervalMs() == null ? 0 : req.getIntervalMs();

        long rtSum = 0;
        int success = 0;
        int fail = 0;

        ProbeRunOnceResponse.ProbeRunOnceLast last = null;

        for (int i = 0; i < times; i++) {
            ProbeResult r = probeOnce(target);

            rtSum += r.rtMs;
            if (r.success) success++; else fail++;

            ProbeEventEntity ev = ProbeEventEntity.builder()
                    .targetId(target.getId())
                    .ts(LocalDateTime.now())
                    .status(r.success ? "SUCCESS" : "FAIL")
                    .httpCode(r.httpCode)
                    .rtMs(r.rtMs)
                    .errorType(r.errorType)
                    .errorMsg(r.errorMsg)
                    .traceId(currentTraceId())
                    .sample(r.sampleJson)
                    .build();

            probeEventMapper.insert(ev);

            last = ProbeRunOnceResponse.ProbeRunOnceLast.builder()
                    .success(r.success)
                    .httpStatus(r.httpCode)
                    .latencyMs((long) r.rtMs)
                    .errorMessage(r.errorMsg)
                    .build();

            if (intervalMs > 0 && i < times - 1) {
                sleepQuietly(intervalMs);
            }
        }

        long avg = times == 0 ? 0 : rtSum / times;

        return ProbeRunOnceResponse.builder()
                .targetId(target.getId())
                .times(times)
                .successCount(success)
                .failureCount(fail)
                .avgLatencyMs(avg)
                .last(last)
                .build();
    }

    private ProbeResult probeOnce(TargetEntity target) {
        String url = buildUrl(target);

        Request request = buildRequest(target,url);

        long startNs = System.nanoTime();

        Integer httpCode = null;
        boolean success = false;
        String errorType = null;
        String errorMsg = null;
        String sampleJson = null;

        String respBodySnippet = null;

        try (Response resp = okHttpClient.newCall(request).execute()) {
            httpCode = resp.code();

            // 读取 body（暂时缩略获取）
            if (resp.body() != null) {
                String body = resp.body().string();
                respBodySnippet = abbreviate(body, 512);
            }

            if (httpCode >= 200 && httpCode < 300) {
                success = true;
            } else {
                success = false;
                errorType = classifyByHttpCode(httpCode);
                errorMsg = "HTTP_" + httpCode;
            }

        } catch (Exception e) {
            success = false;
            errorType = classifyByException(e);
            errorMsg = abbreviate(e.getClass().getSimpleName() + ": " + e.getMessage(), 512);
        }

        int rtMs = (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        // sample JSON
        sampleJson = buildSampleJson(url, httpCode, rtMs, respBodySnippet);

        // 如果是非 2xx 且 errorMsg 为空，补一个
        if (!success && (errorMsg == null || errorMsg.isBlank())) {
            errorMsg = "request failed";
        }

        metrics.recordProbe(target.getEnv(),success,errorType,httpCode,rtMs);

        return new ProbeResult(success, httpCode, rtMs,
                errorType == null ? "UNKNOWN" : errorType,
                errorMsg, sampleJson);
    }

    private Request buildRequest(TargetEntity target, String url) {
        String method = safeUpper(target.getMethod(), "GET");

        Request.Builder b = new Request.Builder().url(url);

        // headers
        Map<String, String> headers = parseHeaders(target.getHeadersJson());
        // 默认 Content-Type（仅当有 body 且未显式指定）
        boolean hasBodyMethod = method.equals("POST") || method.equals("PUT") || method.equals("PATCH");
        if (hasBodyMethod && !containsIgnoreCase(headers, "Content-Type")) {
            headers.put("Content-Type", "application/json");
        }
        headers.forEach(b::addHeader);

        // body
        RequestBody requestBody = null;
        if (hasBodyMethod) {
            String bodyJson = target.getBodyJson();
            if (bodyJson == null || bodyJson.isBlank()) bodyJson = "{}";
            MediaType mt = MediaType.parse(headers.getOrDefault("Content-Type", "application/json"));
            requestBody = RequestBody.create(bodyJson, mt);
        }

        // method binding
        switch (method) {
            case "GET" -> b.get();
            case "POST" -> b.post(requestBody);
            case "PUT" -> b.put(requestBody);
            case "PATCH" -> b.patch(requestBody);
            case "DELETE" -> {
                b.delete();
            }
            case "HEAD" -> b.head();
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        }

        return b.build();
    }

    private Map<String, String> parseHeaders(String headersJson) {
        if (headersJson == null || headersJson.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(headersJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid headers_json: " + abbreviate(headersJson, 128), e);
        }
    }

    private boolean containsIgnoreCase(Map<String,String> map, String key) {
        for (String k : map.keySet()) {
            if (k != null && k.equalsIgnoreCase(key)) return true;
        }
        return false;
    }

    private String safeUpper(String s, String dft) {
        if (s == null || s.isBlank()) return dft;
        return s.trim().toUpperCase();
    }


    private String buildUrl(TargetEntity target) {
        String base = stripTrailingSlash(target.getBaseUrl());
        String path = target.getPath();
        if (path == null || path.isBlank()) path = "/";
        if (!path.startsWith("/")) path = "/" + path;
        return base + path;
    }

    private String buildSampleJson(String url, Integer httpCode, int rtMs, String bodySnippet) {
        try {
            Map<String, Object> m = new HashMap<>();
            m.put("url", url);
            m.put("httpCode", httpCode);
            m.put("rtMs", rtMs);
            if (bodySnippet != null) m.put("bodySnippet", bodySnippet);
            return objectMapper.writeValueAsString(m); // 合法 JSON
        } catch (Exception ignored) {
            return null; // M1 可容错
        }
    }

    private String classifyByHttpCode(int httpCode) {
        if (httpCode >= 500) return "5XX";
        if (httpCode >= 400) return "4XX";
        return "UNKNOWN";
    }

    private String classifyByException(Exception e) {
        String n = e.getClass().getName();
        if (n.contains("SocketTimeoutException")) return "TIMEOUT";
        if (n.contains("UnknownHostException")) return "DNS";
        if (n.contains("ConnectException")) return "CONN";
        return "UNKNOWN";
    }

    private String currentTraceId() {
        // 你如果有链路追踪，在 filter/interceptor 里把 traceId 放 MDC
        // 这里默认取 MDC 的 "traceId"
        return MDC.get("traceId");
    }

    private String stripTrailingSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private void sleepQuietly(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private String abbreviate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private record ProbeResult(boolean success, Integer httpCode, int rtMs,
                               String errorType, String errorMsg, String sampleJson) {}
}
