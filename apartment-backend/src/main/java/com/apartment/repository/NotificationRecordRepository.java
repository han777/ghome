package com.apartment.repository;

import com.apartment.entity.NotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long> {
    List<NotificationRecord> findByStatus(String status);

    List<NotificationRecord> findByStatusIn(List<String> statuses);

    boolean existsByOrderId(Long orderId);

    boolean existsByOrderIdAndKeyBoxNoIsNotNull(Long orderId);
}