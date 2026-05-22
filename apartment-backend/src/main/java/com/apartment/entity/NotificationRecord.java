package com.apartment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_record")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NotificationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private RoomOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "occupy_id")
    @JsonIgnore
    private RoomOccupy occupy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id")
    @JsonIgnore
    private SysUser recipientUser;

    @Column(name = "recipient_email")
    private String recipientEmail; // 直接指定收件邮箱（用于通知邮箱等非系统用户）

    @Column(nullable = false)
    private String channel; // "wecom" or "email"

    @Column(nullable = false)
    private String status = "pending"; // "pending", "sent", "failed", "permanently_failed"

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int retryCount = 0;

    private static final int MAX_RETRIES = 3;

    private String orderNo;
    private String roomNo;
    private String keyBoxNo;
    private String boxPassword;
    @Column(length = 5)
    private String locale;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String recipientName;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String failReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}