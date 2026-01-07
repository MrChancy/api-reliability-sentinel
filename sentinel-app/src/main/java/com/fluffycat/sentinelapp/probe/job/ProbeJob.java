package com.fluffycat.sentinelapp.probe.job;

import com.fluffycat.sentinelapp.common.identity.InstanceIdHolder;
import com.fluffycat.sentinelapp.common.trace.MdcScope;
import com.fluffycat.sentinelapp.common.trace.TraceIdUtil;
import com.fluffycat.sentinelapp.domain.dto.probe.request.ProbeRunOnceRequest;
import com.fluffycat.sentinelapp.domain.entity.target.TargetEntity;
import com.fluffycat.sentinelapp.probe.config.ProbeProperties;
import com.fluffycat.sentinelapp.probe.service.ProbeRunnerService;
import com.fluffycat.sentinelapp.probe.service.ProbeSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProbeJob {

    private final ProbeSchedulerService schedulerService;
    private final ThreadPoolTaskExecutor probeExecutor;
    private final ProbeRunnerService probeRunnerService; // 你已有的“执行 HTTP 探测 + 写 probe_event”的服务

    private final ProbeProperties probeProperties;

    private final AtomicBoolean ticking = new AtomicBoolean(false);

    @Value("${sentinel.enable-job:true}")
    private Boolean enableJob;

    private String ownerId() {
        // 多实例时区分：可以用 hostname + pid，或随机 UUID（启动时生成一次）
        return InstanceIdHolder.ID;
    }

    @Scheduled(fixedDelayString = "${sentinel.probe.tick-ms:1000}")
    public void tick() {
        if (!enableJob) return;

        // 防止 tick 重入（单实例内）
        if (!ticking.compareAndSet(false, true)) return;
        try {
            String owner = ownerId();
            List<TargetEntity> targets = schedulerService.claimDueTargets(probeProperties.env(), probeProperties.batchSize(), probeProperties.leaseSec(), owner);
            if (targets.isEmpty()) return;

            log.info("probe tick claimed {} targets, active={}, queued={}",
                    targets.size(),
                    probeExecutor.getActiveCount(),
                    probeExecutor.getThreadPoolExecutor().getQueue().size());

            for (TargetEntity t : targets) {
                try (MdcScope ignored = MdcScope.of(Map.of(
                        TraceIdUtil.TRACE_ID,TraceIdUtil.newUniqueId(),
                        TraceIdUtil.TARGET_ID,String.valueOf(t.getId())))) {
                    probeExecutor.execute(() -> runOne(t, owner));
                }

            }
        } finally {
            ticking.set(false);
        }
    }

    private void runOne(TargetEntity t, String owner) {
        try {
            ProbeRunOnceRequest req = new ProbeRunOnceRequest();
            req.setTargetId(t.getId());
            probeRunnerService.runOnce(req);
        } catch (Exception e) {
            log.warn("probe failed targetId={}", t.getId(), e);
            // 失败也要推进 next_probe_ts，避免立即重试风暴
        } finally {
            schedulerService.finish(t, owner, LocalDateTime.now());
        }
    }
}

