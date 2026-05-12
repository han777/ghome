package com.apartment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Floor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @ManyToOne
    @JoinColumn(name = "building_id")
    @JsonIgnore
    private Building building;

    /** 编辑标记：added / modified / deleted / unchanged，不持久化 */
    @Transient
    private String _editFlag;
}
