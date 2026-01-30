package com.warehouseManagement.demo.dto;

import lombok.Data;

@Data
public class CartItemDTO {
    private int offerId;
    private String name;
    private int quantity;
    private double unitPrice;
    private double lineTotal;
}
