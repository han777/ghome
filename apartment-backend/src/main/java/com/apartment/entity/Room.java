package com.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "room")
@Data
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String roomNo;

    @ManyToOne
    @JoinColumn(name = "floor_id")
    private Floor floor;

    @ManyToOne
    @JoinColumn(name = "room_type_id")
    private RoomType roomType;

    private String direction; // SOUTH, NORTH

    private Integer status = 0; // 0: Free, 1: Occupied, 2: Locked/Repair
}
