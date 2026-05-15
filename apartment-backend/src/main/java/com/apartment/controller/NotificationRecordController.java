package com.apartment.controller;

import com.apartment.entity.NotificationRecord;
import com.apartment.repository.NotificationRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/notification-records")
public class NotificationRecordController {

    @Autowired
    private NotificationRecordRepository notificationRecordRepository;

    @GetMapping("/paged")
    public Page<NotificationRecord> getPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String recipientName,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo) {

        List<NotificationRecord> all = notificationRecordRepository.findAll();

        List<NotificationRecord> filtered = all;
        if (orderNo != null || recipientName != null || channel != null || status != null
                || createdFrom != null || createdTo != null) {
            filtered = new ArrayList<>();
            for (NotificationRecord nr : all) {
                if (orderNo != null && !orderNo.isBlank()
                        && (nr.getOrderNo() == null || !nr.getOrderNo().contains(orderNo))) continue;
                if (recipientName != null && !recipientName.isBlank()
                        && (nr.getRecipientName() == null || !nr.getRecipientName().contains(recipientName))) continue;
                if (channel != null && !channel.isBlank() && !channel.equals(nr.getChannel())) continue;
                if (status != null && !status.isBlank() && !status.equals(nr.getStatus())) continue;
                if (createdFrom != null && nr.getCreatedAt() != null && nr.getCreatedAt().isBefore(createdFrom)) continue;
                if (createdTo != null && nr.getCreatedAt() != null && nr.getCreatedAt().isAfter(createdTo)) continue;
                filtered.add(nr);
            }
        }

        filtered.sort((a, b) -> {
            LocalDateTime aTime = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
            LocalDateTime bTime = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
            return bTime.compareTo(aTime);
        });

        int total = filtered.size();
        int start = Math.min(page * size, total);
        int end = Math.min(start + size, total);
        List<NotificationRecord> pageContent = filtered.subList(start, end);

        return new PageImpl<>(pageContent, PageRequest.of(page, size), total);
    }
}