package com.example.application.entity.forPayment;

import com.example.application.entity.forEmployee.EmployeeEntity;
import com.example.application.entity.forSale.SaleEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="payments")
@Getter
@Setter
@NoArgsConstructor
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="payment_num")
    private String paymentNum;

    @OneToOne
    @JoinColumn(name="sale_id",nullable = false)
    private SaleEntity sale;

    @Column(name="payment_method")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name="amount")
    private BigDecimal amount;

    @Column(name="paid_at")
    private LocalDateTime paidAt;

    @ManyToOne
    @JoinColumn(name="received_by_employee_id")
    private EmployeeEntity employee;

    public PaymentEntity(String paymentNum,
                         SaleEntity sale,
                         PaymentMethod paymentMethod,
                         BigDecimal amount,
                         LocalDateTime paidAt,
                         EmployeeEntity employee){
        this.paymentNum = paymentNum;
        this.sale = sale;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.paidAt = paidAt;
        this.employee = employee;
    }

}
