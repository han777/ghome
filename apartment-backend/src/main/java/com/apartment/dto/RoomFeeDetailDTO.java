package com.apartment.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RoomFeeDetailDTO {
    private Long id;               // occupy id
    private String orderNo;
    private String roomNo;
    private String roomTypeName;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Integer days;
    private BigDecimal unitPrice;  // actualPrice
    private BigDecimal totalPrice; // actualPrice * quantity
}