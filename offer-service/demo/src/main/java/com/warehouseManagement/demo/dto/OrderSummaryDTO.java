package com.warehouseManagement.demo.dto;

import com.warehouseManagement.demo.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OrderSummaryDTO {
    public int id;
    public String storeName;
    public LocalDate orderDate;
    public String Status;
    public BigDecimal totalPrice;
    public PaymentStatus paymentStatus;
}
