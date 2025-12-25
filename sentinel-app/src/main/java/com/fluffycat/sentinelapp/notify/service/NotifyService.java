package com.fluffycat.sentinelapp.notify.service;

import com.fluffycat.sentinelapp.alert.repo.AlertEventMapper;
import com.fluffycat.sentinelapp.common.constants.DbValues;
import com.fluffycat.sentinelapp.domain.entity.alert.AlertEventEntity;
import com.fluffycat.sentinelapp.domain.entity.notify.NotifyLogEntity;
import com.fluffycat.sentinelapp.domain.entity.target.TargetEntity;
import com.fluffycat.sentinelapp.notify.config.NotifyProperties;
import com.fluffycat.sentinelapp.notify.notifier.EmailNotifier;
import com.fluffycat.sentinelapp.notify.repo.NotifyLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotifyService {

    private final NotifyLogMapper notifyLogMapper;
    private final AlertEventMapper alertEventMapper;
    private final EmailNotifier emailNotifier;
    private final NotifyProperties props;

    public void sendEmail(Long alertId, TargetEntity target, String subject, String body) {
        String to = resolveReceiver(target);
        try {
            emailNotifier.send(to, subject, body);
            notifyLogMapper.insert(NotifyLogEntity.builder()
                    .alertId(alertId)
                    .channel(DbValues.NotifyChannel.EMAIL)
                    .receiver(to)
                    .status(DbValues.NotifyStatus.SENT)
                    .errorMsg(null)
                    .build());

            // 更新 last_sent_ts（不要求在同一事务里）
            AlertEventEntity upd = new AlertEventEntity();
            upd.setId(alertId);
            upd.setLastSentTs(LocalDateTime.now());
            alertEventMapper.updateById(upd);

        } catch (Exception e) {
            notifyLogMapper.insert(NotifyLogEntity.builder()
                    .alertId(alertId)
                    .channel(DbValues.NotifyChannel.EMAIL)
                    .receiver(to)
                    .status(DbValues.NotifyStatus.FAIL)
                    .errorMsg(abbrev(e.getClass().getSimpleName() + ": " + e.getMessage(), 512))
                    .build());
        }
    }

    private String resolveReceiver(TargetEntity target) {
        String owner = target.getOwner();
        if (owner != null && owner.contains("@")) return owner;
        return props.defaultTo();
    }

//    public void sendEmailForNewOpen(long alertId, TargetEntity target) {
//        String to = resolveReceiver(target);
//        String subject = buildSubject(alertId, target);
//        String body = buildBody(alertId, target);
//
//        try {
//            emailNotifier.send(to, subject, body);
//
//            notifyLogMapper.insert(NotifyLogEntity.builder()
//                    .alertId(alertId)
//                    .channel(DbValues.NotifyChannel.EMAIL)
//                    .receiver(to)
//                    .status(DbValues.NotifyStatus.SENT)
//                    .errorMsg(null)
//                    .build());
//
//            // 更新 last_sent_ts（不要求在同一事务里）
//            AlertEventEntity upd = new AlertEventEntity();
//            upd.setId(alertId);
//            upd.setLastSentTs(LocalDateTime.now());
//            alertEventMapper.updateById(upd);
//
//        } catch (Exception e) {
//            notifyLogMapper.insert(NotifyLogEntity.builder()
//                    .alertId(alertId)
//                    .channel(DbValues.NotifyChannel.EMAIL)
//                    .receiver(to)
//                    .status(DbValues.NotifyStatus.FAIL)
//                    .errorMsg(abbrev(e.getClass().getSimpleName() + ": " + e.getMessage(), 512))
//                    .build());
//            // 不抛出，避免影响告警事件落库与下一轮扫描
//        }
//    }



    private String abbrev(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}

