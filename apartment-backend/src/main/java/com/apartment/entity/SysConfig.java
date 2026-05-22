package com.apartment.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "sys_config")
@Data
public class SysConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_emails", columnDefinition = "TEXT")
    private String notificationEmails;  // 通知邮箱（逗号分隔）

    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "updated_by")
    @JsonIgnoreProperties({"roles", "password"})
    private SysUser updatedBy;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
