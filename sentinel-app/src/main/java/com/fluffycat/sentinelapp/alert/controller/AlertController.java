package com.fluffycat.sentinelapp.alert.controller;

import com.fluffycat.sentinelapp.alert.service.AlertService;
import com.fluffycat.sentinelapp.common.api.Result;
import com.fluffycat.sentinelapp.domain.dto.alert.response.AlertResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/demo/alert")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @PostMapping("/{id}/ack")
    public ResponseEntity<Result<AlertResponse>> ack(@PathVariable String id){
        AlertResponse r = alertService.ack(id);
        return ResponseEntity.ok(Result.success(r));
    }

    @PostMapping("/{id}/resolved")
    public ResponseEntity<Result<AlertResponse>> resolved(@PathVariable String id){
        AlertResponse r = alertService.resolved(id);
        return ResponseEntity.ok(Result.success(r));
    }
}
