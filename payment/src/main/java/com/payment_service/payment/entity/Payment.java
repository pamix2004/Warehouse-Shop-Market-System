package com.payment_service.payment.entity;

import com.payment_service.payment.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "payment")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.pending;



    // Added the Stripe Session ID mapping
    @Column(name = "stripe_session_id")
    private String stripeSessionId;
}