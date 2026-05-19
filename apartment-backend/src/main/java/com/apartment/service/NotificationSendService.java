package com.apartment.service;

import com.apartment.entity.NotificationRecord;
import com.apartment.entity.SysUser;
import com.apartment.exception.BusinessException;
import com.apartment.exception.ErrorCode;
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

    @Autowired
    private EmailService emailService;

    @Autowired
    private MessageTemplateService messageTemplateService;

    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void sendPendingNotifications() {
        List<NotificationRecord> records = notificationRecordRepository.findByStatusIn(Arrays.asList("pending", "failed"));
        if (records.isEmpty()) return;

        log.info("Processing {} notification records (pending + failed)", records.size());

        for (NotificationRecord nr : records) {
            try {
                // Resolve recipient and re-validate channel against current user source
                SysUser recipient = nr.getRecipientUser();
                if (recipient == null) {
                    throw new BusinessException(ErrorCode.NOTIF_RECIPIENT_MISSING);
                }

                String effectiveChannel = nr.getChannel();
                // If stored channel is "wecom" but user is manually created (no wecomId), force email
                if ("wecom".equals(effectiveChannel) && (recipient.getWecomId() == null || recipient.getWecomId().isBlank())) {
                    log.warn("Notification {} has channel=wecom but recipient {} has no wecomId, falling back to email", nr.getId(), recipient.getUsername());
                    effectiveChannel = "email";
                }

                if ("wecom".equals(effectiveChannel)) {
                    weComService.sendTextMessage(recipient.getWecomId(), nr.getContent());
                } else if ("email".equals(effectiveChannel)) {
                    if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
                        throw new BusinessException(ErrorCode.NOTIF_RECIPIENT_MISSING);
                    }
                    String userLocale = nr.getLocale() != null ? nr.getLocale() :
                        (recipient.getLocale() != null ? recipient.getLocale() : "zh");
                    String subject = messageTemplateService.buildEmailSubject(userLocale);
                    emailService.sendEmail(recipient.getEmail(), subject, nr.getContent());
                } else {
                    throw new BusinessException(ErrorCode.NOTIF_UNKNOWN_CHANNEL);
                }

                nr.setStatus("sent");
                nr.setFailReason(null);
                nr.setRetryCount(nr.getRetryCount() + 1);
                notificationRecordRepository.save(nr);
                log.info("Notification {} sent successfully via {} (attempts: {})", nr.getId(), nr.getChannel(), nr.getRetryCount());

            } catch (BusinessException e) {
                // Non-retryable business errors: mark as permanently failed
                nr.setRetryCount(nr.getRetryCount() + 1);
                nr.setFailReason(e.getErrorCode().getCode());
                nr.setStatus("permanently_failed");
                notificationRecordRepository.save(nr);
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
