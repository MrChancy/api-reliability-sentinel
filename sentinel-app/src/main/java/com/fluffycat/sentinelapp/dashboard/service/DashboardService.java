package com.fluffycat.sentinelapp.dashboard.service;

import com.fluffycat.sentinelapp.domain.dto.dashboard.response.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardService {

    public TargetsOverviewResponse getTargetsOverview(String env) {
        // M3-01: 先返回空结构，M3-02 再接 SQL 聚合
        return TargetsOverviewResponse.builder()
                .env(env)
                .windowMinutes(5)
                .generatedAt(LocalDateTime.now())
                .items(List.of())
                .build();
    }

    public AlertsOverviewResponse getAlertsOverview(String env, String status) {
        // M3-01: 占位
        return AlertsOverviewResponse.builder()
                .env(env)
                .status(status)
                .generatedAt(LocalDateTime.now())
                .items(List.of())
                .build();
    }

    public TargetTimeseriesResponse getTargetTimeseries(Long targetId, String env) {
        // M3-01: 占位（M3-04 实现）
        return TargetTimeseriesResponse.builder()
                .env(env)
                .targetId(targetId)
                .rangeMinutes(60)
                .bucketSec(60)
                .generatedAt(LocalDateTime.now())
                .points(List.of())
                .errorTypeBreakdown(List.of())
                .build();
    }
}
