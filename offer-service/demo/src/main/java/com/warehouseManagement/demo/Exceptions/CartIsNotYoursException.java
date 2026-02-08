package com.warehouseManagement.demo.Exceptions;

public class CartIsNotYoursException extends RuntimeException {
    public CartIsNotYoursException(String message) {
        super(message);
    }
}
