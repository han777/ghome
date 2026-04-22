package com.apartment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sys_dict_item")
@Data
public class SysDictItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dict_id")
    @JsonIgnore
    private SysDict dict;

    @Column(nullable = false)
    private String itemLabel;

    @Column(nullable = false)
    private String itemValue;

    private Integer sortOrder = 0;

    private String description;
}
