package com.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sys_dept")
@Data
public class SysDepartment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String deptName;

    private Long parentId;

    private Integer sortOrder = 0;
}
