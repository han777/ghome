package com.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
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

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("order")
    private java.util.List<RoomOccupy> roomOccupies;

    private Integer customerType = 1; // 1: Individual, 2: Group
    private Integer bizType; // 1: Short, 2: Long
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Integer status = 0; // 0: Cooling-off, 1: Pending, 2: In, 3: Out, 4: Canceled
    private BigDecimal totalAmount;
    private BigDecimal roomFee;
    private BigDecimal serviceFee;
    @ManyToOne
    @JoinColumn(name = "book_user_id")
    private SysUser booker;

    private String bookPhone;
    
    private String remarks;

    private String company;
    private String costCenter;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<OrderProductDetail> productDetails;

    @Transient
    private Integer roomCount;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
