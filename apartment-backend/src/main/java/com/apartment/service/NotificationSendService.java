package com.apartment.service;

import com.apartment.entity.NotificationRecord;
import com.apartment.entity.SysUser;
import com.apartment.repository.NotificationRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationSendService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSendService.class);

    @Autowired
    private NotificationRecordRepository notificationRecordRepository;

    @Autowired
    private WeComService weComService;

    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void sendPendingNotifications() {
        List<NotificationRecord> pending = notificationRecordRepository.findByStatus("pending");
        if (pending.isEmpty()) return;

        log.info("Processing {} pending notification records", pending.size());

        for (NotificationRecord nr : pending) {
            try {
                if ("wecom".equals(nr.getChannel())) {
                    SysUser recipient = nr.getRecipientUser();
                    if (recipient == null || recipient.getWecomId() == null || recipient.getWecomId().isBlank()) {
                        throw new RuntimeException("Recipient wecomId is null or blank");
                    }
                    weComService.sendTextMessage(recipient.getWecomId(), nr.getContent());
                } else if ("email".equals(nr.getChannel())) {
                    // Email sending not implemented yet
                    throw new RuntimeException("Email channel not implemented yet");
                } else {
                    throw new RuntimeException("Unknown channel: " + nr.getChannel());
                }

                nr.setStatus("sent");
                nr.setFailReason(null);
                notificationRecordRepository.save(nr);
                log.info("Notification {} sent successfully via {}", nr.getId(), nr.getChannel());

            } catch (Exception e) {
                nr.setStatus("failed");
                nr.setFailReason(e.getMessage());
                notificationRecordRepository.save(nr);
                log.warn("Notification {} failed: {}", nr.getId(), e.getMessage());
            }
        }
    }
}