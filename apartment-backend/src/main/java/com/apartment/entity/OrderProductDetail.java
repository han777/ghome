package com.apartment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "order_product_detail")
@Data
public class OrderProductDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private RoomOrder order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductPrice product;

    private BigDecimal actualPrice;
    private Integer quantity = 1;
    private LocalDate consumeDate;
    private String remarks;
}
