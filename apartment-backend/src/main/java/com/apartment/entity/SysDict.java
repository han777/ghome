package com.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "sys_dict")
@Data
public class SysDict {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String dictCode;

    @Column(nullable = false)
    private String dictName;

    private String description;

    @OneToMany(mappedBy = "dict", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SysDictItem> items;
}
