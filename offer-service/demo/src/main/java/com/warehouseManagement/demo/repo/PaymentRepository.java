package com.warehouseManagement.demo.repo;

import com.warehouseManagement.demo.PaymentStatus;
import com.warehouseManagement.demo.entity.Payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    // --- common useful queries ---

    // if you used @ManyToOne private Orders order;


    Optional<Payment> findByPaymentId(int paymentId);

    List<Payment> findByStatus(PaymentStatus status);


}
