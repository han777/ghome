package com.apartment.repository;

import com.apartment.entity.ScheduledTaskLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduledTaskLogRepository extends JpaRepository<ScheduledTaskLog, Long> {
}
