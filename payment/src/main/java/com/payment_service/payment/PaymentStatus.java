package com.payment_service.payment;

public enum PaymentStatus {
    pending,
    processing,
    succeeded,
    failed,
    refunded,
    partially_refunded,
    canceled,
    requires_action
}