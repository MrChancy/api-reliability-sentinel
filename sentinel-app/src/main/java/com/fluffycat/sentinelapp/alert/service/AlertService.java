package com.fluffycat.sentinelapp.alert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fluffycat.sentinelapp.alert.AlertEventConverter;
import com.fluffycat.sentinelapp.alert.repo.AlertEventMapper;
import com.fluffycat.sentinelapp.common.api.ErrorCode;
import com.fluffycat.sentinelapp.common.constants.ConstantText;
import com.fluffycat.sentinelapp.common.constants.DbValues;
import com.fluffycat.sentinelapp.common.exception.BusinessException;
import com.fluffycat.sentinelapp.common.pagination.PageRequest;
import com.fluffycat.sentinelapp.common.pagination.PageRequests;
import com.fluffycat.sentinelapp.common.pagination.PageResponse;
import com.fluffycat.sentinelapp.common.trace.MdcScope;
import com.fluffycat.sentinelapp.domain.dto.alert.response.AlertResponse;
import com.fluffycat.sentinelapp.domain.entity.alert.AlertEventEntity;
import com.fluffycat.sentinelapp.domain.entity.target.TargetEntity;
import com.fluffycat.sentinelapp.target.repo.TargetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final TargetMapper targetMapper;
    private final AlertEventMapper alertEventMapper;
    private final AlertInternalProcessor alertInternalProcessor;
    private final PageRequests pageRequests;


    public int scanErrorRateAndUpsert() {

        // 只取 enabled=1 且配置了 error_rate_threshold 的 target
        List<TargetEntity> targets = targetMapper.selectList(
                Wrappers.<TargetEntity>lambdaQuery()
                        .eq(TargetEntity::getEnabled, 1)
                        .isNotNull(TargetEntity::getErrorRateThreshold)
        );

        int affected = 0;

        for (TargetEntity t : targets) {
            try (MdcScope ignored = MdcScope.of(Map.of(
                    ConstantText.TARGET_ID,String.valueOf(t.getId())
            ))) {
                affected += alertInternalProcessor.processSingle(t);
            }
        }

        return affected;
    }

    public AlertResponse ack(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "invalid id for ack");
        }
        AlertEventEntity entity = setAlertEventStatus(id, DbValues.AlertStatus.ACK);

        return AlertResponse.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public AlertResponse resolved(Long id) {
        AlertEventEntity entity = setAlertEventStatus(id, DbValues.AlertStatus.RESOLVED);

        return AlertResponse.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public AlertEventEntity setAlertEventStatus(Long alertId, String status) {
        AlertEventEntity entity = alertEventMapper.selectById(alertId);

        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "update alert event not found! alertId: " + alertId);
        }

        entity.setStatus(status);

        if (alertEventMapper.updateById(entity) > 0) {
            return entity;
        } else {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "update alert event error! alertId: " + alertId);
        }
    }

    public PageResponse<AlertResponse> getAlerts(String status, Long targetId, Integer page, Integer size) {
        PageRequest pr = pageRequests.of(page, size);

        LambdaQueryWrapper<AlertEventEntity> qw = Wrappers.lambdaQuery();
        qw.eq(status != null, AlertEventEntity::getStatus, status)
                .eq(targetId != null, AlertEventEntity::getTargetId, targetId);

        if (pr.isUnpaged()){
            List<AlertResponse> alertEventEntities = alertEventMapper.selectList(qw).stream()
                    .map(AlertEventConverter::toResponse)
                    .toList();
            return PageResponse.unpaged(alertEventEntities);
        }
        Page<AlertEventEntity> p = alertEventMapper.selectPage(Page.of(pr.getPage(), pr.getSize()), qw);
        return PageResponse.from(p, AlertEventConverter::toResponse);
    }
}


