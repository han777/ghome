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
    private String roomCardNo; // 房间卡号
    private String doorCode; // 门锁码
    private Integer occupantCount; // 入住人数
    private String coOccupants; // 同住人
    
    private Integer status = 0; // 0: Current (当前), 1: Finish (完成)

    @Transient
    private String roomNo;
    @Transient
    private String roomTypeName;
}
