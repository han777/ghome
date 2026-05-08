package com.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "room_type")
@Data
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String typeCode;

    /**
     * 多语言名称，以 JSONB 存储，格式: {"zh": "大床房", "en": "King Room"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "name_intl_json", columnDefinition = "jsonb")
    private String nameIntlJson;

    private BigDecimal priceShortRent;
    private BigDecimal priceLongRent;

    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private java.util.List<RoomTypeImage> images;

    private String remarks;
}
