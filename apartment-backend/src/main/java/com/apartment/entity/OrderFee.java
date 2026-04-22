package com.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_fee")
@Data
public class OrderFee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private RoomOrder order;

    private String feeType; // e.g., Breakfast, Damage, etc.
    private BigDecimal amount;
    private String remarks;
    private LocalDateTime createdAt;
}
