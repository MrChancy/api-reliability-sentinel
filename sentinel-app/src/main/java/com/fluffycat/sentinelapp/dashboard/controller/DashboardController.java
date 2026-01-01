package com.fluffycat.sentinelapp.dashboard.controller;

import com.fluffycat.sentinelapp.common.api.Result;
import com.fluffycat.sentinelapp.dashboard.service.DashboardService;
import com.fluffycat.sentinelapp.domain.dto.dashboard.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard/targets")
    public ResponseEntity<Result<TargetsOverviewResponse>> getTargetsOverview(
            @RequestParam(required = false) String env
    ) {
        TargetsOverviewResponse resp = dashboardService.getTargetsOverview(env);
        return ResponseEntity.ok(Result.success(resp));
    }

    @GetMapping("/dashboard/alerts")
    public ResponseEntity<Result<AlertsOverviewResponse>> getAlertsOverview(
            @RequestParam(required = false) String env,
            @RequestParam(defaultValue = "OPEN") String status
    ) {
        AlertsOverviewResponse resp = dashboardService.getAlertsOverview(env, status);
        return ResponseEntity.ok(Result.success(resp));
    }

    @GetMapping("/targets/{id}/dashboard/timeseries")
    public ResponseEntity<Result<TargetTimeseriesResponse>> getTargetTimeseries(
            @PathVariable("id") Long targetId,
            @RequestParam(required = false) String env
    ) {
        TargetTimeseriesResponse resp = dashboardService.getTargetTimeseries(targetId, env);
        return ResponseEntity.ok(Result.success(resp));
    }
}
