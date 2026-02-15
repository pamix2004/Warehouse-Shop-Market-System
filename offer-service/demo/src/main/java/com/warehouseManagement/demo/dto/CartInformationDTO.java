package com.warehouseManagement.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
@Data
public class CartInformationDTO {
    private int cartId;
    private int storeId;
    private String wholesalerName;
    private List<CartItemDTO> items;
    private BigDecimal cartTotal;

    // getters/setters
}
