package com.warehouseManagement.demo.dto;

import java.math.BigDecimal;

public class OfferFormDTO {

    // ===== OFFER =====
    private Integer productId; // null = nowy produkt
    private BigDecimal price;
    private int available_quantity;
    private int minimal_quantity;

    // ===== NEW PRODUCT =====
    private String productName;
    private Integer producerId;
    private Integer categoryId;

    // ===== getters / setters =====

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getAvailable_quantity() {
        return available_quantity;
    }

    public int getMinimal_quantity() {
        return minimal_quantity;
    }

    public String getProductName() {
        return productName;
    }

    public Integer getProducerId() {
        return producerId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setAvailable_quantity(int available_quantity) {
        this.available_quantity = available_quantity;
    }

    public void setMinimal_quantity(int minimal_quantity) {
        this.minimal_quantity = minimal_quantity;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setProducerId(Integer producerId) {
        this.producerId = producerId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }
}
