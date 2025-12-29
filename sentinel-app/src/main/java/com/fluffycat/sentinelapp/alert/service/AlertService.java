package com.fluffycat.sentinelapp.alert.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fluffycat.sentinelapp.domain.entity.target.TargetEntity;
import com.fluffycat.sentinelapp.target.repo.TargetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final TargetMapper targetMapper;
    private final AlertInternalProcessor alertInternalProcessor;


    public int scanErrorRateAndUpsert() {

        // 只取 enabled=1 且配置了 error_rate_threshold 的 target
        List<TargetEntity> targets = targetMapper.selectList(
                Wrappers.<TargetEntity>lambdaQuery()
                        .eq(TargetEntity::getEnabled, 1)
                        .isNotNull(TargetEntity::getErrorRateThreshold)
        );

        int affected = 0;

        for (TargetEntity t : targets) {
            affected += alertInternalProcessor.processSingle(t);
        }

        return affected;
    }

}


