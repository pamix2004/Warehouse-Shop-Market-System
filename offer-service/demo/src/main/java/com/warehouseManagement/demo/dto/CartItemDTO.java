package com.warehouseManagement.demo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemDTO {
    private int offerId;
    private String name;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
