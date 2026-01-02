package com.fluffycat.sentinelapp.dashboard.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fluffycat.sentinelapp.common.env.EnvResolver;
import com.fluffycat.sentinelapp.common.pagination.PageRequest;
import com.fluffycat.sentinelapp.common.pagination.PageRequests;
import com.fluffycat.sentinelapp.common.pagination.PageResponse;
import com.fluffycat.sentinelapp.dashboard.config.DashboardProperties;
import com.fluffycat.sentinelapp.dashboard.repo.DashboardMapper;
import com.fluffycat.sentinelapp.domain.dto.dashboard.response.*;
import com.fluffycat.sentinelapp.domain.entity.target.TargetEntity;
import com.fluffycat.sentinelapp.domain.enums.alert.AlertEventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardMapper dashboardMapper;
    private final EnvResolver envResolver;
    private final DashboardProperties dashboardProperties;
    private final PageRequests pageRequests;

    public TargetsOverviewResponse getTargetsOverview(String env) {
        String resolvedEnv = envResolver.resolve(env);
        int tWindowMin = dashboardProperties.targetWindowMinutes();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusMinutes(tWindowMin);

        List<TargetEntity> targets = dashboardMapper.selectTargetsByEnv(resolvedEnv);
        if (targets.isEmpty()) {
            return TargetsOverviewResponse.builder()
                    .env(resolvedEnv)
                    .windowMinutes(tWindowMin)
                    .generatedAt(now)
                    .items(List.of())
                    .build();
        }

        List<Long> ids = targets.stream().map(TargetEntity::getId).filter(Objects::nonNull).toList();
        String idsCsv = ids.stream().map(String::valueOf).collect(Collectors.joining(","));

        // 近 5m 聚合
        Map<Long, DashboardMapper.TargetAggRow> aggMap = dashboardMapper.selectTargetAgg5m(idsCsv, start, now)
                .stream().collect(Collectors.toMap(DashboardMapper.TargetAggRow::getTargetId, x -> x));

        // p95
        Map<Long, DashboardMapper.TargetP95Row> p95Map = dashboardMapper.selectTargetP95_5m(idsCsv, start, now)
                .stream().collect(Collectors.toMap(DashboardMapper.TargetP95Row::getTargetId, x -> x));

        // 最新告警（如果没有记录，则 NONE）
        Map<Long, DashboardMapper.TargetAlertRow> alertMap = dashboardMapper.selectLatestAlertByTargets(idsCsv)
                .stream().collect(Collectors.toMap(DashboardMapper.TargetAlertRow::getTargetId, x -> x));

        List<TargetOverviewItem> items = new ArrayList<>(targets.size());
        for (TargetEntity t : targets) {
            Long tid = t.getId();

            DashboardMapper.TargetAggRow agg = aggMap.get(tid);
            long total = agg == null || agg.getTotalCnt() == null ? 0 : agg.getTotalCnt();
            long fail = agg == null || agg.getFailCnt() == null ? 0 : agg.getFailCnt();
            LocalDateTime lastProbeTs = agg == null ? null : agg.getLastProbeTs();

            BigDecimal successRatePct = calcSuccessRatePct(total, fail);

            DashboardMapper.TargetP95Row p95 = p95Map.get(tid);
            Integer p95RtMs = p95 == null ? null : p95.getP95RtMs();

            DashboardMapper.TargetAlertRow a = alertMap.get(tid);
            String alertStatus = (a == null || a.getStatus() == null) ? "NONE" : a.getStatus();

            items.add(TargetOverviewItem.builder()
                    .targetId(tid)
                    .name(t.getName())
                    .method(t.getMethod())
                    .baseUrl(t.getBaseUrl())
                    .path(t.getPath())
                    .enabled(t.getEnabled())
                    .owner(t.getOwner())
                    .tags(t.getTags())
                    .silencedUntil(t.getSilencedUntil())
                    .totalCnt(total)
                    .failCnt(fail)
                    .successRatePct(successRatePct)
                    .p95RtMs(p95RtMs)
                    .lastProbeTs(lastProbeTs)
                    .alertStatus(alertStatus)
                    .alertLastSeenTs(a == null ? null : a.getLastSeenTs())
                    .alertLastSentTs(a == null ? null : a.getLastSentTs())
                    .build());
        }

        return TargetsOverviewResponse.builder()
                .env(resolvedEnv)
                .windowMinutes(5)
                .generatedAt(now)
                .items(items)
                .build();
    }

    public PageResponse<AlertOverviewItem> getAlertsOverview(String env, AlertEventStatus status, Integer page, Integer size) {
        String resolvedEnv = envResolver.resolve(env);

        PageRequest pageRequest = pageRequests.of(page, size);

        Page<DashboardMapper.AlertOverviewRow> rows =
                dashboardMapper.selectAlertsOverview(Page.of(pageRequest.getPage(),pageRequest.getSize()),resolvedEnv, status);

        return PageResponse.from(rows, null);
    }

    public TargetTimeseriesResponse getTargetTimeseries(Long targetId, String env) {
        String resolvedEnv = envResolver.resolve(env); // 你现有 env 解析逻辑即可
        LocalDateTime now = LocalDateTime.now();

        int rangeMinutes = 60;
        int bucketSec = 60;

        LocalDateTime end = truncateToBucket(now, bucketSec).plusSeconds(bucketSec); // 保证包含当前分钟
        LocalDateTime start = end.minusMinutes(rangeMinutes);

        // 1) 查聚合
        List<DashboardMapper.TimeseriesAggRow> aggRows =
                dashboardMapper.selectTargetTimeseriesAgg(targetId, start, end, bucketSec);

        // 2) 查 p95
        List<DashboardMapper.TimeseriesP95Row> p95Rows =
                dashboardMapper.selectTargetTimeseriesP95(targetId, start, end, bucketSec);

        // 3) 查错误类型分布
        List<DashboardMapper.ErrorTypeBreakdownRow> breakdownRows =
                dashboardMapper.selectErrorTypeBreakdown(targetId, start, end);

        Map<LocalDateTime, TimeseriesPoint> pointMap = new HashMap<>();
        for (DashboardMapper.TimeseriesAggRow r : aggRows) {
            long total = r.getTotalCnt() == null ? 0 : r.getTotalCnt();
            long fail = r.getFailCnt() == null ? 0 : r.getFailCnt();

            TimeseriesPoint p = TimeseriesPoint.builder()
                    .bucketTs(r.getBucketTs())
                    .totalCnt(total)
                    .failCnt(fail)
                    .errorRatePct(calcRatePct(fail, total))
                    .avgRtMs(r.getAvgRtMs())
                    .p95RtMs(null)
                    .build();

            pointMap.put(r.getBucketTs(), p);
        }

        for (DashboardMapper.TimeseriesP95Row r : p95Rows) {
            TimeseriesPoint p = pointMap.get(r.getBucketTs());
            if (p != null) {
                p.setP95RtMs(r.getP95RtMs());
            } else {
                // 某个 bucket 只有 p95（极少见），也补一个点
                pointMap.put(r.getBucketTs(), TimeseriesPoint.builder()
                        .bucketTs(r.getBucketTs())
                        .totalCnt(0L)
                        .failCnt(0L)
                        .errorRatePct(calcRatePct(0, 0))
                        .avgRtMs(null)
                        .p95RtMs(r.getP95RtMs())
                        .build());
            }
        }

        // 4) 补齐空 bucket（图表用）
        List<TimeseriesPoint> points = new ArrayList<>();
        for (LocalDateTime ts = start; ts.isBefore(end); ts = ts.plusSeconds(bucketSec)) {
            TimeseriesPoint p = pointMap.get(ts);
            if (p == null) {
                p = TimeseriesPoint.builder()
                        .bucketTs(ts)
                        .totalCnt(0L)
                        .failCnt(0L)
                        .errorRatePct(calcRatePct(0, 0))
                        .avgRtMs(null)
                        .p95RtMs(null)
                        .build();
            }
            points.add(p);
        }

        List<ErrorTypeBreakdownItem> breakdown = breakdownRows.stream()
                .map(x -> ErrorTypeBreakdownItem.builder()
                        .errorType(x.getErrorType())
                        .count(x.getCnt())
                        .build())
                .toList();

        return TargetTimeseriesResponse.builder()
                .env(resolvedEnv)
                .targetId(targetId)
                .rangeMinutes(rangeMinutes)
                .bucketSec(bucketSec)
                .generatedAt(now)
                .points(points)
                .errorTypeBreakdown(breakdown)
                .build();
    }

    private LocalDateTime truncateToBucket(LocalDateTime t, int bucketSec) {
        long epoch = t.atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
        long aligned = (epoch / bucketSec) * bucketSec;
        return LocalDateTime.ofEpochSecond(aligned, 0, java.time.ZoneOffset.systemDefault().getRules().getOffset(t));
    }

    private java.math.BigDecimal calcRatePct(long fail, long total) {
        if (total <= 0) return java.math.BigDecimal.ZERO;
        return java.math.BigDecimal.valueOf(fail)
                .multiply(java.math.BigDecimal.valueOf(100))
                .divide(java.math.BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP);
    }


    private BigDecimal calcSuccessRatePct(long total, long fail) {
        if (total <= 0) return BigDecimal.ZERO;
        long success = total - fail;
        return BigDecimal.valueOf(success)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
