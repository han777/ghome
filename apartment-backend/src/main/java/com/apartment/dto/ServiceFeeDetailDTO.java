package com.apartment.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ServiceFeeDetailDTO {
    private Long id;
    private String orderNo;
    private String productName;
    private BigDecimal standardPrice;   // product.price
    private BigDecimal actualPrice;
    private Integer quantity;
    private BigDecimal amount;           // actualPrice * quantity
}