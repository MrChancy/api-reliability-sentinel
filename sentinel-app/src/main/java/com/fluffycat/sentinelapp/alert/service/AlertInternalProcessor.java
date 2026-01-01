package com.fluffycat.sentinelapp.alert.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluffycat.sentinelapp.alert.repo.AlertEventMapper;
import com.fluffycat.sentinelapp.common.constants.DbValues;
import com.fluffycat.sentinelapp.domain.entity.alert.AlertEventEntity;
import com.fluffycat.sentinelapp.domain.entity.probe.ProbeEventEntity;
import com.fluffycat.sentinelapp.domain.entity.target.TargetEntity;
import com.fluffycat.sentinelapp.domain.enums.alert.AlertUpsertAction;
import com.fluffycat.sentinelapp.probe.repo.ProbeEventMapper;
import com.fluffycat.sentinelcommon.service.NotificationPort;
import com.fluffycat.sentinelcommon.service.NotifyPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

import static com.fluffycat.sentinelapp.domain.enums.alert.AlertUpsertAction.OPEN_CREATED;
import static com.fluffycat.sentinelapp.domain.enums.alert.AlertUpsertAction.REOPENED;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertInternalProcessor {

    private final ProbeEventMapper probeEventMapper;
    private final AlertEventMapper alertEventMapper;
    private final ObjectMapper objectMapper;
    private final Optional<NotificationPort> notificationPort;
    private final NotifyPolicy notifyPolicy;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int processSingle(TargetEntity t) {
        LocalDateTime now = LocalDateTime.now();

        int windowSec = t.getWindowSec() == null ? 300 : t.getWindowSec();
        LocalDateTime windowStart = now.minusSeconds(windowSec);

        // 探测事件表，取总查询次数
        long totalCnt = probeEventMapper.selectCount(
                Wrappers.<ProbeEventEntity>lambdaQuery()
                        .eq(ProbeEventEntity::getTargetId, t.getId())
                        .ge(ProbeEventEntity::getTs, windowStart)
        );

        if (totalCnt <= 0)
            return 0;

        // 取失败次数
        long failCnt = probeEventMapper.selectCount(
                Wrappers.<ProbeEventEntity>lambdaQuery()
                        .eq(ProbeEventEntity::getTargetId, t.getId())
                        .ge(ProbeEventEntity::getTs, windowStart)
                        .eq(ProbeEventEntity::getStatus, DbValues.ProbeStatus.FAIL)
        );

        // 失败率：百分比（DECIMAL(5,2)）对比
        // threshold 是 50.00 表示 50%
        double errorRatePct = (failCnt * 100.0) / totalCnt;
        double thresholdPct = t.getErrorRateThreshold().doubleValue();
        String dedupeKey = sha1(t.getId() + ":" + DbValues.AlertType.ERROR_RATE);

        // 超过失败告警阈值则更新状态并发送邮件，否则设置状态为已解决
        if (errorRatePct >= thresholdPct) {
            AlertInternalProcessor.AlertUpsertResult r = upsertOpenOrReexisting(now, windowStart, t, totalCnt, failCnt, errorRatePct, thresholdPct, dedupeKey);

            AlertEventEntity lastAlert = alertEventMapper.selectById(r.alertId());

            if (lastAlert==null)
                return 0;


            NotifyPolicy.NotifyDecision decision = notifyPolicy.decide(
                    new NotifyPolicy.NotifyContext(
                            r.action().name(),
                            lastAlert.getStatus(),
                            lastAlert.getLastSentTs(),
                            t.getSilencedUntil(),
                            now
                    )
            );

            if (!decision.shouldSend()) {
                log.info("skip notify: alertId={}, reason={}", r.alertId(), decision.reason());
                return 0;
            }

            notificationPort.ifPresentOrElse(
                    port -> port.enqueue(new NotificationPort.AlertNotificationCommand(
                            r.alertId(),
                            t.getId(),
                            lastAlert.getDedupeKey(),
                            decision.reason(),
                            r.summary(),
                            r.detailsJson(),
                            t.getEnv(),
                            now
                    )),
                    ()-> log.info("NotificationPort absent, enqueue skipped: alertId={}", r.alertId())
            );

            return 1;
        } else {
            resolveIfOpen(now, windowStart, t, totalCnt, failCnt, errorRatePct, thresholdPct, dedupeKey);
            return 1;
        }
    }

    private AlertInternalProcessor.AlertUpsertResult upsertOpenOrReexisting(LocalDateTime now,
                                                                  LocalDateTime windowStart,
                                                                  TargetEntity t,
                                                                  long totalCnt,
                                                                  long failCnt,
                                                                  double errorRatePct,
                                                                  double thresholdPct,
                                                                  String dedupeKey) {

        String alertType = DbValues.AlertType.ERROR_RATE;
        String summary = String.format(
                "Name:%s, ERROR_RATE breach: %.2f%% >= %.2f%%, window=%ds, total=%d, fail=%d, target=%s %s",
                t.getName(),errorRatePct, thresholdPct, t.getWindowSec(), totalCnt, failCnt, t.getBaseUrl(), t.getPath()
        );
        String detailsJson = buildDetailsJson(now, windowStart, t, totalCnt, failCnt, errorRatePct, thresholdPct);

        AlertEventEntity existing = alertEventMapper.selectOne(
                Wrappers.<AlertEventEntity>lambdaQuery()
                        .eq(AlertEventEntity::getDedupeKey, dedupeKey)
                        .last("LIMIT 1")
        );
        List<String> unsolved = List.of(DbValues.AlertStatus.OPEN,DbValues.AlertStatus.ACK);
        if (existing != null && unsolved.contains(existing.getStatus())) {
            AlertEventEntity upd = new AlertEventEntity();
            upd.setId(existing.getId());
            upd.setLastSeenTs(now);
            upd.setCountInWindow(existing.getCountInWindow() == null ? 1 : existing.getCountInWindow() + 1);
            upd.setSummary(abbrev(summary, 512));
            upd.setDetailsJson(detailsJson);
            alertEventMapper.updateById(upd);

            return new AlertInternalProcessor.AlertUpsertResult(AlertUpsertAction.OPEN_UPDATED, existing.getId(),summary,detailsJson);
        }

        if (existing != null && DbValues.AlertStatus.RESOLVED.equals(existing.getStatus()))
        {
            AlertEventEntity upd = new AlertEventEntity();
            upd.setId(existing.getId());
            upd.setStatus(DbValues.AlertStatus.OPEN);
            upd.setFirstSeenTs(now);          // REOPEN：重置
            upd.setLastSeenTs(now);
            upd.setCountInWindow(1);          // REOPEN：重置
            upd.setLastSentTs(null);          // 清空，方便重发策略
            upd.setSummary(abbrev(summary, 512));
            upd.setDetailsJson(detailsJson);
            alertEventMapper.updateById(upd);

            return new AlertInternalProcessor.AlertUpsertResult(REOPENED, existing.getId(),summary,detailsJson);
        }

        // 3) 都不存在 -> 插入 OPEN
        AlertEventEntity ev = AlertEventEntity.builder()
                .targetId(t.getId())
                .alertType(alertType)
                .alertLevel(DbValues.AlertLevel.P1)
                .dedupeKey(dedupeKey)
                .status(DbValues.AlertStatus.OPEN)
                .firstSeenTs(now)
                .lastSeenTs(now)
                .countInWindow(1)
                .summary(abbrev(summary, 512))
                .detailsJson(detailsJson)
                .build();

        try {
            alertEventMapper.insert(ev);
            return new AlertUpsertResult(OPEN_CREATED, ev.getId(),summary,detailsJson);
        } catch (DuplicateKeyException ex) {
            // 并发兜底
            AlertEventEntity justOpen = alertEventMapper.selectOne(
                    Wrappers.<AlertEventEntity>lambdaQuery()
                            .eq(AlertEventEntity::getDedupeKey, dedupeKey)
                            .eq(AlertEventEntity::getStatus, DbValues.AlertStatus.OPEN)
                            .last("LIMIT 1")
            );
            if (justOpen != null) {
                AlertEventEntity upd = new AlertEventEntity();
                upd.setId(justOpen.getId());
                upd.setLastSeenTs(now);
                upd.setCountInWindow(justOpen.getCountInWindow() == null ? 1 : justOpen.getCountInWindow() + 1);
                upd.setSummary(abbrev(summary, 512));
                upd.setDetailsJson(detailsJson);
                alertEventMapper.updateById(upd);
                return new AlertInternalProcessor.AlertUpsertResult(AlertUpsertAction.OPEN_UPDATED, justOpen.getId(),summary,detailsJson);
            }
            throw ex;
        }
    }


    private int resolveIfOpen(LocalDateTime now,
                              LocalDateTime windowStart,
                              TargetEntity t,
                              long totalCnt,
                              long failCnt,
                              double errorRatePct,
                              double thresholdPct,
                              String dedupeKey) {

        AlertEventEntity cur = alertEventMapper.selectOne(
                Wrappers.<AlertEventEntity>lambdaQuery()
                        .eq(AlertEventEntity::getDedupeKey, dedupeKey)
                        .in(AlertEventEntity::getStatus, DbValues.AlertStatus.OPEN, DbValues.AlertStatus.ACK)
                        .last("LIMIT 1")
        );
        if (cur == null) return 0;

        String summary = String.format(
                "RECOVERED: %.2f%% < %.2f%%, window=%ds, total=%d, fail=%d, target=%s %s",
                errorRatePct, thresholdPct, t.getWindowSec(), totalCnt, failCnt, t.getBaseUrl(), t.getPath()
        );
        String detailsJson = buildDetailsJson(now, windowStart, t, totalCnt, failCnt, errorRatePct, thresholdPct);

        AlertEventEntity upd = new AlertEventEntity();
        upd.setId(cur.getId());
        upd.setStatus(DbValues.AlertStatus.RESOLVED);
        upd.setLastSeenTs(now);
        upd.setSummary(abbrev(summary, 512));
        upd.setDetailsJson(detailsJson);
        alertEventMapper.updateById(upd);

        return 1;
    }


    private String buildDetailsJson(LocalDateTime now,
                                    LocalDateTime windowStart,
                                    TargetEntity t,
                                    long totalCnt,
                                    long failCnt,
                                    double errorRatePct,
                                    double thresholdPct) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("targetId", t.getId());
            m.put("name", t.getName());
            m.put("env", t.getEnv());
            m.put("method", t.getMethod());
            m.put("url", t.getBaseUrl() + t.getPath());
            m.put("windowStart", windowStart.toString());
            m.put("windowEnd", now.toString());
            m.put("windowSec", t.getWindowSec());
            m.put("totalCnt", totalCnt);
            m.put("failCnt", failCnt);
            m.put("errorRatePct", round2(errorRatePct));
            m.put("thresholdPct", round2(thresholdPct));
            m.put("silencedUntil", t.getSilencedUntil() == null ? null : t.getSilencedUntil().toString());
            m.put("silenceReason", t.getSilenceReason());
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            return null;
        }
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private String sha1(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(b);
        } catch (Exception e) {
            return s;
        }
    }

    private String abbrev(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    public record AlertUpsertResult(AlertUpsertAction action, long alertId,String summary,String detailsJson) {}
}