package com.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "room_type")
@Data
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String typeCode;

    @ElementCollection
    @CollectionTable(name = "room_type_name_intl", joinColumns = @JoinColumn(name = "room_type_id"))
    @MapKeyColumn(name = "lang")
    @Column(name = "name")
    private Map<String, String> nameIntl;

    private BigDecimal priceShortRent;
    private BigDecimal priceLongRent;
    
    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private java.util.List<RoomTypeImage> images;

    private String remarks;
}
