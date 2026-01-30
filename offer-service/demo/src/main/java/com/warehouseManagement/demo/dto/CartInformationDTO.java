package com.warehouseManagement.demo.dto;

import lombok.Data;

import java.util.List;
@Data
public class CartInformationDTO {
    private int cartId;
    private int storeId;
    private String wholesalerName;
    private List<CartItemDTO> items;
    private double cartTotal;

    // getters/setters
}
