package com.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_occupy")
@Data
public class RoomOccupy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("roomOccupies")
    private RoomOrder order;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne
    @JoinColumn(name = "occupant_user_id")
    private SysUser occupantUser;

    private LocalDateTime checkInTime; // 实际入住时间
    private LocalDateTime checkOutTime; // 实际离开时间
    private Integer occupantCount; // 入住人数
    private String coOccupants; // 同住人
    private String occupantName; // 入住人姓名（团体时=团体名称）

    private Integer status = 0; // 0: Pending(待入住), 1: Checked-in(已入住), 2: Checked-out(已退房), 3: Canceled(取消)

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_CHECKED_IN = 1;
    public static final int STATUS_CHECKED_OUT = 2;
    public static final int STATUS_CANCELED = 3;
    
    private java.math.BigDecimal actualPrice; // 实际单价
    private Integer quantity; // 数量（天数/次数）

    @Transient
    private String roomNo;
    @Transient
    private String roomTypeName;
    @Transient
    private String _action; // "add", "modify", "delete", "unchanged"
}
