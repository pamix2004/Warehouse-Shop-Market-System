package com.warehouseManagement.demo.Exceptions;

public class CartsAreEmpty extends RuntimeException {
    public CartsAreEmpty(String message) {
        super(message);
    }
}
