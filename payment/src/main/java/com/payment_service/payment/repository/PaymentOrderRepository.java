package com.payment_service.payment.repository;

import com.payment_service.payment.entity.Payment;
import com.payment_service.payment.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, PaymentOrder.PaymentOrderId> {

    @Query("SELECT pay FROM Payment pay " +
            "JOIN PaymentOrder po ON pay.paymentId = po.id.paymentId " +
            "WHERE po.id.orderId = :orderId")
    Payment findPaymentByOrderId(@Param("orderId") Integer orderId);
}