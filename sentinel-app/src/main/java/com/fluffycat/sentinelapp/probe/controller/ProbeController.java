package com.fluffycat.sentinelapp.probe.controller;

import com.fluffycat.sentinelapp.common.api.Result;
import com.fluffycat.sentinelapp.domain.dto.probe.request.ProbeRunOnceRequest;
import com.fluffycat.sentinelapp.domain.dto.probe.response.ProbeRunOnceResponse;
import com.fluffycat.sentinelapp.probe.service.ProbeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/probe")
@RequiredArgsConstructor
public class ProbeController {

    private final ProbeService probeService;

    @PostMapping("/run-once")
    public ResponseEntity<Result<ProbeRunOnceResponse>> runOnce(@Valid @RequestBody ProbeRunOnceRequest request) {
        return ResponseEntity.ok(Result.success(probeService.runOnce(request)));
    }
}
