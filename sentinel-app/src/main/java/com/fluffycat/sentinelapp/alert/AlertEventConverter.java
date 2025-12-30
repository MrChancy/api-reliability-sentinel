package com.fluffycat.sentinelapp.alert;

import com.fluffycat.sentinelapp.domain.dto.alert.response.AlertResponse;
import com.fluffycat.sentinelapp.domain.entity.alert.AlertEventEntity;

public final class AlertEventConverter {
    private AlertEventConverter(){}

    public static AlertResponse toResponse(AlertEventEntity e){
        if (e==null)
            return null;

        return AlertResponse.builder()
                .id(e.getId())
                .targetId(e.getTargetId())
                .firstSeenTs(e.getFirstSeenTs())
                .lastSeenTs(e.getLastSeenTs())
                .lastSentTs(e.getLastSentTs())
                .summary(e.getSummary())
                .status(e.getStatus())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
