package com.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sys_menu")
@Data
public class SysMenu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String menuName;

    private String path;
    private String component;
    private String icon;
    private Long parentId;
    private Integer sortOrder = 0;
    private String permission; // e.g., sys:user:list
}
