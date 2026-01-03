package com.fluffycat.sentinelapp.notify.service;

import com.fluffycat.sentinelapp.alert.repo.AlertEventMapper;
import com.fluffycat.sentinelapp.common.constants.DbValues;
import com.fluffycat.sentinelapp.common.metrics.SentinelMetrics;
import com.fluffycat.sentinelapp.common.util.EmailUtils;
import com.fluffycat.sentinelapp.domain.entity.alert.AlertEventEntity;
import com.fluffycat.sentinelapp.domain.entity.notify.NotifyLogEntity;
import com.fluffycat.sentinelapp.domain.entity.target.TargetEntity;
import com.fluffycat.sentinelapp.notify.config.NotifyProperties;
import com.fluffycat.sentinelapp.notify.notifier.EmailNotifier;
import com.fluffycat.sentinelapp.notify.repo.NotifyLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotifyService {

    private final NotifyLogMapper notifyLogMapper;
    private final AlertEventMapper alertEventMapper;
    private final EmailNotifier emailNotifier;
    private final NotifyProperties notifyProperties;
    private final SentinelMetrics metrics;

    public void sendEmail(Long alertId, TargetEntity target, String subject, String body) {
        String[] to = resolveReceiver(target);
        try {

            emailNotifier.send(to, subject, body);
            metrics.incNotifySend("EMAIL", true);
            notifyLogMapper.insert(NotifyLogEntity.builder()
                    .alertId(alertId)
                    .channel(DbValues.NotifyChannel.EMAIL)
                    .receiver(String.join(",",to))
                    .status(DbValues.NotifyStatus.SENT)
                    .errorMsg(null)
                    .build());

            // 更新 last_sent_ts（不要求在同一事务里）
            AlertEventEntity upd = new AlertEventEntity();
            upd.setId(alertId);
            upd.setLastSentTs(LocalDateTime.now());
            alertEventMapper.updateById(upd);

        } catch (Exception e) {
            metrics.incNotifySend("EMAIL", false);
            notifyLogMapper.insert(NotifyLogEntity.builder()
                    .alertId(alertId)
                    .channel(DbValues.NotifyChannel.EMAIL)
                    .receiver(String.join(",",to))
                    .status(DbValues.NotifyStatus.FAIL)
                    .errorMsg(abbrev(e.getClass().getSimpleName() + ": " + e.getMessage(), 512))
                    .build());
        }
    }

    private String[] resolveReceiver(TargetEntity target) {

        //先走owner，直接责任人
        String owner = target.getOwner();
        String[] receivers = Arrays.stream(StringUtils.commaDelimitedListToStringArray(owner))
                .map(String::trim)
                .filter(EmailUtils::isEmail)
                .toArray(String[]::new);

        if (receivers.length > 0)
            return receivers;

        //再走tag,支持多业务组，组内多个邮箱
        receivers = Arrays.stream(StringUtils.commaDelimitedListToStringArray(target.getTags()))
                .map(String::trim)
                .map(tag -> notifyProperties.tagRoutes().get(tag))
                .filter(Objects::nonNull)
                .flatMap(emails -> Arrays.stream(StringUtils.commaDelimitedListToStringArray(emails)))
                .map(String::trim)
                .filter(EmailUtils::isEmail)
                .toArray(String[]::new);
        if (receivers.length > 0)
            return receivers;

        //再走env，生产环境值班组，其余开发组
        String envEmail = notifyProperties.envRoutes().get(target.getEnv());
        if(StringUtils.hasText(envEmail)){
            return new String[]{envEmail};
        }

        //最后走默认，开发组
        return new String[]{notifyProperties.defaultTo()};
    }


    private String abbrev(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}

