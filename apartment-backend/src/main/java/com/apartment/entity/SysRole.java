package com.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sys_role")
@Data
public class SysRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String roleName;

    @Column(unique = true, nullable = false)
    private String roleCode;

    private String description;
}
