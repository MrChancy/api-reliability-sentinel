package com.fluffycat.sentinelapp.target.controller;

import com.fluffycat.sentinelapp.common.api.Result;
import com.fluffycat.sentinelapp.common.pagination.PageResponse;
import com.fluffycat.sentinelapp.domain.dto.target.request.CreateTargetRequest;
import com.fluffycat.sentinelapp.domain.dto.target.request.UpdateTargetRequest;
import com.fluffycat.sentinelapp.domain.dto.target.response.TargetResponse;
import com.fluffycat.sentinelapp.target.service.TargetService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Targets", description = "监控目标管理")
@RestController
@RequestMapping("/api/targets")
@RequiredArgsConstructor
public class TargetController {

    private final TargetService targetService;

    @PostMapping
    public ResponseEntity<Result<TargetResponse>> createTarget(@Valid @RequestBody CreateTargetRequest request) {
        TargetResponse created = targetService.createTarget(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(created));
    }

    @GetMapping
    public ResponseEntity<Result<PageResponse<TargetResponse>>> getAllTargets(@RequestParam(required = false) Boolean enabled,
                                                                              @RequestParam(required = false) String tag,
                                                                              @RequestParam(required = false) String owner,
                                                                              @RequestParam(required = false) String env,
                                                                              @RequestParam(defaultValue = "1") Integer page,
                                                                              @RequestParam(defaultValue = "20") Integer size) {
        PageResponse<TargetResponse> targets = targetService.getTargets(enabled, tag, owner, env, page, size);
        return ResponseEntity.ok(Result.success(targets));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Result<TargetResponse>> updateTarget(
            @PathVariable Long id,
            @RequestBody UpdateTargetRequest request) {
        TargetResponse updated = targetService.updateTarget(id, request);
        return ResponseEntity.status(HttpStatus.OK).body(Result.success(updated));
    }


}