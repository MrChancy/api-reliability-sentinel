package com.fluffycat.sentinelapp.alert.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fluffycat.sentinelapp.alert.repo.AlertEventMapper;
import com.fluffycat.sentinelapp.common.api.ErrorCode;
import com.fluffycat.sentinelapp.common.constants.DbValues;
import com.fluffycat.sentinelapp.common.exception.BusinessException;
import com.fluffycat.sentinelapp.domain.dto.alert.response.AlertResponse;
import com.fluffycat.sentinelapp.domain.entity.alert.AlertEventEntity;
import com.fluffycat.sentinelapp.domain.entity.target.TargetEntity;
import com.fluffycat.sentinelapp.target.repo.TargetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final TargetMapper targetMapper;
    private final AlertEventMapper alertEventMapper;
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

    public AlertResponse ack(String id) {
        AlertEventEntity entity = setAlertEventStatus(id,DbValues.AlertStatus.ACK);

        return AlertResponse.builder()
                .id(String.valueOf(entity.getId()))
                .status(entity.getStatus())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public AlertResponse resolved(String id) {
        AlertEventEntity entity = setAlertEventStatus(id,DbValues.AlertStatus.RESOLVED);

        return AlertResponse.builder()
                .id(String.valueOf(entity.getId()))
                .status(entity.getStatus())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public AlertEventEntity setAlertEventStatus(String id, String status){
        Long alertId = Long.valueOf(id);
        AlertEventEntity entity = alertEventMapper.selectById(alertId);

        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "update alert event not found! alertId: " + id);
        }

        entity.setStatus(status);

        if(alertEventMapper.updateById(entity) > 0){
            return entity;
        }else{
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "update alert event error! alertId: " + id);
        }
    }
}


