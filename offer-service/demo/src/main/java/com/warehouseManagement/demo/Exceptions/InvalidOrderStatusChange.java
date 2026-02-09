package com.warehouseManagement.demo.Exceptions;

public class InvalidOrderStatusChange extends RuntimeException {
    public InvalidOrderStatusChange(String message) {
        super(message);
    }
}
