package com.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "product_price")
@Data
public class ProductPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;
    private String unit; // 计量单位
    private BigDecimal price;
    
    /**
     * Category: 1: Sale (出售), 2: Damage (损坏)
     */
    private Integer category; 
    
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
}
