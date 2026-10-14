package com.example.application.repository.payment;

import com.example.application.entity.forPayment.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    boolean existsByPaymentNum(String paymentNum);
}
