package com.fluffycat.sentinelapp.dashboard.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fluffycat.sentinelapp.common.env.EnvResolver;
import com.fluffycat.sentinelapp.common.pagination.PageRequest;
import com.fluffycat.sentinelapp.common.pagination.PageRequests;
import com.fluffycat.sentinelapp.common.pagination.PageResponse;
import com.fluffycat.sentinelapp.dashboard.config.DashboardProperties;
import com.fluffycat.sentinelapp.dashboard.repo.DashboardMapper;
import com.fluffycat.sentinelapp.domain.dto.dashboard.response.AlertOverviewItem;
import com.fluffycat.sentinelapp.domain.dto.dashboard.response.TargetOverviewItem;
import com.fluffycat.sentinelapp.domain.dto.dashboard.response.TargetTimeseriesResponse;
import com.fluffycat.sentinelapp.domain.dto.dashboard.response.TargetsOverviewResponse;
import com.fluffycat.sentinelapp.domain.entity.target.TargetEntity;
import com.fluffycat.sentinelapp.domain.enums.alert.AlertEventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        throw new UnsupportedOperationException();
    }

    private BigDecimal calcSuccessRatePct(long total, long fail) {
        if (total <= 0) return BigDecimal.ZERO;
        long success = total - fail;
        return BigDecimal.valueOf(success)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
