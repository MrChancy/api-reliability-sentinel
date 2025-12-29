package com.fluffycat.sentinelapp.notify.publish;

import com.fluffycat.sentinelcommon.service.NotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InProcessEventNotificationPort implements NotificationPort {
    private final ApplicationEventPublisher publisher;

    @Override
    public void enqueue(AlertNotificationCommand cmd) {
        publisher.publishEvent(cmd);
    }
}
