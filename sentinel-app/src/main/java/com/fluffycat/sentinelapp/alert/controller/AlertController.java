package com.fluffycat.sentinelapp.alert.controller;

import com.fluffycat.sentinelapp.alert.service.AlertService;
import com.fluffycat.sentinelapp.common.api.Result;
import com.fluffycat.sentinelapp.common.pagination.PageResponse;
import com.fluffycat.sentinelapp.domain.dto.alert.response.AlertResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/demo/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;


    @GetMapping
    public ResponseEntity<Result<PageResponse<AlertResponse>>> alerts(@RequestParam(required = false) String status,
                                                                      @RequestParam(required = false) Long targetId,
                                                                      @RequestParam(defaultValue = "1") Integer page,
                                                                      @RequestParam(defaultValue = "20") Integer size) {
        PageResponse<AlertResponse> r = alertService.getAlerts(status, targetId,page,size);
        return ResponseEntity.ok(Result.success(r));
    }

    @PostMapping("/{id}/ack")
    public ResponseEntity<Result<AlertResponse>> ack(@PathVariable Long id) {
        AlertResponse r = alertService.ack(id);
        return ResponseEntity.ok(Result.success(r));
    }

    @PostMapping("/{id}/resolved")
    public ResponseEntity<Result<AlertResponse>> resolved(@PathVariable Long id) {
        AlertResponse r = alertService.resolved(id);
        return ResponseEntity.ok(Result.success(r));
    }
}
