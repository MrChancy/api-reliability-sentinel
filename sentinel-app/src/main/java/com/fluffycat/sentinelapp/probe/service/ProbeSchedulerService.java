package com.fluffycat.sentinelapp.probe.service;

import com.fluffycat.sentinelapp.domain.entity.target.TargetEntity;
import com.fluffycat.sentinelapp.probe.repo.ProbeTargetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProbeSchedulerService {

    private final ProbeTargetMapper probeTargetMapper;

    @Transactional
    public List<TargetEntity> claimDueTargets(String env, int batchSize, int leaseSec, String owner) {
        List<TargetEntity> due = probeTargetMapper.selectDueForUpdateSkipLocked(env, batchSize);
        for (TargetEntity t : due) {
            probeTargetMapper.acquireLease(t.getId(), leaseSec, owner);
            t.setLeaseOwner(owner);
        }
        return due;
    }

    public LocalDateTime computeNextProbeTs(LocalDateTime plannedNext,
                                            LocalDateTime now,
                                            int intervalSec) {
        // fixed-rate + max protection
        LocalDateTime candidate = plannedNext.plusSeconds(intervalSec);
        LocalDateTime minNext = now.plusSeconds(intervalSec);
        return candidate.isAfter(minNext) ? candidate : minNext;
    }

    public void finish(TargetEntity t, String owner, LocalDateTime now) {
        LocalDateTime plannedNext = t.getNextProbeTs();
        int intervalSec = t.getIntervalSec() == null ? 60 : t.getIntervalSec();
        LocalDateTime next = computeNextProbeTs(plannedNext, now, intervalSec);
        probeTargetMapper.finishAndRelease(t.getId(), owner, now, next);
    }
}
