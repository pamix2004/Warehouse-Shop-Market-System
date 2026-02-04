package com.warehouseManagement.demo.repo;

import com.warehouseManagement.demo.entity.PaymentOrder;
import com.warehouseManagement.demo.entity.PaymentOrderId;
import com.warehouseManagement.demo.entity.Order;
import com.warehouseManagement.demo.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, PaymentOrderId> {

    Optional<PaymentOrder> findByOrder(Order order);

    List<PaymentOrder> findByPayment(Payment payment);

    // Matches paymentId from Payment entity and orderId from Order entity
    boolean existsByPayment_PaymentIdAndOrder_OrderId(int paymentId, int orderId);
}