package com.warehouseManagement.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "payment_order")
@IdClass(PaymentOrderId.class)
public class PaymentOrder {

    @Id
    @ManyToOne
    @JoinColumn(name = "payment_id") // DB column name
    private Payment payment;

    @Id
    @ManyToOne
    @JoinColumn(name = "order_id")   // DB column name
    private Order order;

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    @Override
    public String toString() {
        return "PaymentOrder{" +
                "paymentId=" + (payment != null ? payment.getPaymentId() : "null") +
                ", orderId=" + (order != null ? order.getOrderId() : "null") +
                '}';
    }
}