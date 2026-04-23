package com.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_order")
@Data
public class RoomOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNo;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private SysUser user;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    private Integer bizType; // 1: Short, 2: Long
    private LocalDate startDate;
    private LocalDate endDate;

    private Integer status = 0; // 0: Locking, 1: Pending, 2: In, 3: Out, 4: Canceled
    private String doorCode;
    private BigDecimal totalAmount;
    private String guestPhone;
    
    // New fields
    private String checkInTime = "14:00";
    private String checkOutTime = "13:59";
    private String roomCardCode;
    private String checkInType;
    private Integer occupantCount = 1;
    private String coOccupants;
    private String remarks;

    private String company;
    private String costCenter;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
