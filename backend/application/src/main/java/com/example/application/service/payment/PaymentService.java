package com.example.application.service.payment;


import com.example.application.repository.payment.PaymentRepository;
import org.springframework.stereotype.Service;

import static com.example.application.config.AppConfig.*;
import static com.example.application.config.AppConfig.CHARS;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    private String generatedUniqueNum() {
        String Num;
        do {
            Num = generateRandomString(len);
        } while (paymentRepository.existsByPaymentNum(Num));
        return Num;
    }


}
