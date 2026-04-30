package com.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_maintenance")
@Data
public class RoomMaintenance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // 0: In Progress (维修中), 1: Completed (已完成), 2: Cancelled (已取消)
    private Integer status = 0;
}
