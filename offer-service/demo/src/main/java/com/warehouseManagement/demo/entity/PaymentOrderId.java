package com.warehouseManagement.demo.entity;

import java.io.Serializable;
import java.util.Objects;

public class PaymentOrderId implements Serializable {
    private int payment; // matches field name in PaymentOrder
    private int order;   // matches field name in PaymentOrder

    public PaymentOrderId() {}

    public PaymentOrderId(int payment, int order) {
        this.payment = payment;
        this.order = order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaymentOrderId that)) return false;
        return payment == that.payment && order == that.order;
    }

    @Override
    public int hashCode() {
        return Objects.hash(payment, order);
    }
}