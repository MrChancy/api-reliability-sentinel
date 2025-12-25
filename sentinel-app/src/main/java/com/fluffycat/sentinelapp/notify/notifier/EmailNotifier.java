package com.fluffycat.sentinelapp.notify.notifier;

import com.fluffycat.sentinelapp.notify.config.NotifyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailNotifier {

    private final JavaMailSender mailSender;
    private final NotifyProperties props;

    public void send(String to, String subject, String content) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(props.from());
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(content);
        mailSender.send(msg);
    }
}

