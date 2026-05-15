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

import java.util.Arrays;
import java.util.List;

@Service
public class NotificationSendService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSendService.class);
    private static final int MAX_RETRIES = 3;

    @Autowired
    private NotificationRecordRepository notificationRecordRepository;

    @Autowired
    private WeComService weComService;

    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void sendPendingNotifications() {
        List<NotificationRecord> records = notificationRecordRepository.findByStatusIn(Arrays.asList("pending", "failed"));
        if (records.isEmpty()) return;

        log.info("Processing {} notification records (pending + failed)", records.size());

        for (NotificationRecord nr : records) {
            try {
                if ("wecom".equals(nr.getChannel())) {
                    SysUser recipient = nr.getRecipientUser();
                    if (recipient == null || recipient.getWecomId() == null || recipient.getWecomId().isBlank()) {
                        throw new RuntimeException("Recipient wecomId is null or blank");
                    }
                    weComService.sendTextMessage(recipient.getWecomId(), nr.getContent());
                } else if ("email".equals(nr.getChannel())) {
                    throw new RuntimeException("Email channel not implemented yet");
                } else {
                    throw new RuntimeException("Unknown channel: " + nr.getChannel());
                }

                nr.setStatus("sent");
                nr.setFailReason(null);
                nr.setRetryCount(nr.getRetryCount() + 1);
                notificationRecordRepository.save(nr);
                log.info("Notification {} sent successfully via {} (attempts: {})", nr.getId(), nr.getChannel(), nr.getRetryCount());

            } catch (Exception e) {
                int newRetryCount = nr.getRetryCount() + 1;
                nr.setRetryCount(newRetryCount);
                nr.setFailReason(e.getMessage());
                if (newRetryCount >= MAX_RETRIES) {
                    nr.setStatus("permanently_failed");
                    log.error("Notification {} permanently failed after {} retries: {}", nr.getId(), newRetryCount, e.getMessage());
                } else {
                    nr.setStatus("failed");
                    log.warn("Notification {} failed (attempt {}/{}): {}", nr.getId(), newRetryCount, MAX_RETRIES, e.getMessage());
                }
                notificationRecordRepository.save(nr);
            }
        }
    }
}