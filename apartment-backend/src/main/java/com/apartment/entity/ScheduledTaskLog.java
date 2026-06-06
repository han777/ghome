package com.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_task_log")
@Data
public class ScheduledTaskLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_name", nullable = false, length = 200)
    private String taskName;

    @Column(name = "execute_time", nullable = false)
    private LocalDateTime executeTime;

    private Long duration;

    @Column(nullable = false, length = 20)
    private String status = "success";

    @Column(name = "fail_reason", columnDefinition = "TEXT")
    private String failReason;

    @PrePersist
    protected void onCreate() {
        if (executeTime == null) executeTime = LocalDateTime.now();
    }
}
