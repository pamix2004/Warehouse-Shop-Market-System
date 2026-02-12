package com.payment_service.payment.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "payment_order")
public class PaymentOrder {

    @EmbeddedId
    private PaymentOrderId id;

    public PaymentOrder() {}

    public PaymentOrderId getId() { return id; }
    public void setId(PaymentOrderId id) { this.id = id; }

    @Embeddable
    public static class PaymentOrderId implements Serializable {
        @Column(name = "payment_id")
        private Integer paymentId;

        @Column(name = "order_id")
        private Integer orderId;

        public PaymentOrderId() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PaymentOrderId that)) return false;
            return Objects.equals(paymentId, that.paymentId) && Objects.equals(orderId, that.orderId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(paymentId, orderId);
        }
    }
}