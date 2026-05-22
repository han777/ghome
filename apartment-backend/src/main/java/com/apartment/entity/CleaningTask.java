package com.apartment.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cleaning_task")
@Data
public class CleaningTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_id")
    @JsonIgnoreProperties({"roomType", "building"})
    private Room room;

    @Column(nullable = false)
    private Integer taskType;  // 1=日常保洁, 2=强打扫

    private LocalDateTime generatedAt;  // 生成时间

    private Long generatedBy;  // 生成人ID

    private LocalDateTime startTime;  // 开始时间

    private LocalDateTime endTime;  // 结束时间

    private LocalDateTime completedAt;  // 报工时间

    private String content;  // 任务内容

    @Column(nullable = false)
    private Integer status = 0;  // 0=计划, 1=取消, 2=完成

    @Column(nullable = false)
    private LocalDate taskDate;  // 任务日期（用于去重）

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }
}
