package com.example.application.entity.forSale;


import com.example.application.entity.forCustomer.CustomerEntity;
import com.example.application.entity.forEmployee.EmployeeEntity;
import com.example.application.entity.forReservation.ReservationEntity;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="sales")
@Getter
@Setter
@NoArgsConstructor
public class SaleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;


    @Column(name="sale_num", unique = true, nullable = false)
    private String saleNum;

    @ManyToOne
    @JoinColumn(name="customer_id",nullable = false)
    private CustomerEntity customer;

    @ManyToOne
    @JoinColumn(name="seller_employee_id", nullable = false)
    private EmployeeEntity employee;

    @Column(name="status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SaleStatus status;

    @OneToOne
    @JoinColumn(name="reservation_id")
    private ReservationEntity reservation;

    @Column(name="total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name="paid_at")
    private LocalDateTime paidAt;

    public SaleEntity(String saleNum,
                      CustomerEntity customer,
                      EmployeeEntity employee,
                      SaleStatus status,
                      ReservationEntity reservation,
                      BigDecimal totalAmount,
                      LocalDateTime paidAt,
                      LocalDateTime createdAt) {
        this.saleNum = saleNum;
        this.customer = customer;
        this.employee = employee;
        this.status = status;
        this.reservation = reservation;
        this.totalAmount = totalAmount;
        this.paidAt = paidAt;
        this.createdAt = createdAt;
    }
}
