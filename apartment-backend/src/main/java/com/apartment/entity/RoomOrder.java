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
    @JoinColumn(name = "book_user_id")
    private SysUser booker;

    @ManyToOne
    @JoinColumn(name = "last_update_user_id")
    private SysUser lastUpdateUser;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("order")
    private java.util.List<RoomOccupy> roomOccupies;

    private Integer customerType = 1; // 1: Individual, 2: Group
    private Integer bizType; // 1: Short, 2: Long
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Integer status = 0; // 0: Cooling-off, 1: Pending, 2: In, 3: Out, 4: Canceled
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal roomFee = BigDecimal.ZERO;
    private BigDecimal serviceFee = BigDecimal.ZERO;
    private String bookPhone;
    
    private String remarks;

    private String company;
    private String costCenter;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    /** 创建人：新建订单时赋值为当前登录用户 */
    @ManyToOne
    @JoinColumn(name = "create_user_id")
    private SysUser createUser;

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
