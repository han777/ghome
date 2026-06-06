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

    private Integer customerType; // 1: Individual, 2: Group (default set in controller)
    private Integer bizType; // 1: Short, 2: Long

    @ManyToOne
    @JoinColumn(name = "purpose_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private BookingPurpose purpose;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Integer status; // 0: Cooling-off, 1: Pending, 2: In, 3: Out, 4: Canceled (default set in controller)
    private BigDecimal totalAmount;
    private BigDecimal roomFee;
    private BigDecimal serviceFee;
    private String bookPhone;

    private String keyBoxNo;     // 房卡箱号
    private String boxPassword;  // 箱密码

    private String remarks;

    private String company;
    private String costCenter;

    private String groupName;       // 团体名称（customerType=2 时必填）
    private String contactName;     // 联系人姓名（团体时必填）
    private String contactPhone;    // 联系电话（团体时必填，区别于 bookPhone）
    private String activityCode;    // 活动编码（财务对账用）
    private String projectCode;     // 项目编码（财务对账用）
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    /** 创建人：新建订单时赋值为当前登录用户 */
    @ManyToOne
    @JoinColumn(name = "create_user_id")
    private SysUser createUser;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<OrderProductDetail> productDetails;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.util.List<OrderLog> orderLogs;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.util.List<NotificationRecord> notificationRecords;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.util.List<OrderFee> orderFees;

    @Transient
    private Integer roomCount;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
