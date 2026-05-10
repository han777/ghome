package com.apartment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_log")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OrderLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private RoomOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "roles", "wecomId"})
    private SysUser operator;

    @Column(nullable = false)
    private LocalDateTime operationTime;

    @Column(nullable = false)
    private String operationType; // SAVE, SEND_CARD, CANCEL, CHECKOUT, CHANGE_ROOM, ADJUST_TIME

    @Column(columnDefinition = "TEXT")
    private String operationContent;

    @Column(columnDefinition = "TEXT")
    private String changedFields; // JSON string for SAVE operations: [{"field":"name","oldValue":"x","newValue":"y"}]
}
