package com.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "room_type_image")
@Data
public class RoomTypeImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_type_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private RoomType roomType;

    private String url;
    private Integer sortOrder = 0;
}
