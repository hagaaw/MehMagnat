package com.example.application.entity.forReservation;


import com.example.application.entity.forCustomer.CustomerEntity;
import com.example.application.entity.forEmployee.EmployeeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="reservations")
@Getter
@Setter
@NoArgsConstructor
public class ReservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="reservation_num", unique = true, nullable = false)
    private String reservationNum;


    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;

    @ManyToOne
    @JoinColumn(name = "created_by_employeed_id")
    private EmployeeEntity employee;

    @Column(name="status",nullable = false)
    private ReservationStatus status;

    @Column(name="start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name="end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name="comment", nullable = false)
    private String comment;

    public ReservationEntity(String reservationNum,
                             CustomerEntity customer,
                             EmployeeEntity employee,
                             ReservationStatus status,
                             LocalDateTime startAt,
                             LocalDateTime endAt,
                             LocalDateTime createdAt,
                             String comment) {
        this.reservationNum = reservationNum;
        this.customer = customer;
        this.employee = employee;
        this.status = status;
        this.startAt = startAt;
        this.endAt = endAt;
        this.createdAt = createdAt;
        this.comment = comment;
    }

    public ReservationEntity(
                             CustomerEntity customer,
                             EmployeeEntity employee,
                             ReservationStatus status,
                             LocalDateTime startAt,
                             LocalDateTime endAt,
                             LocalDateTime createdAt,
                             String comment) {
        this.customer = customer;
        this.employee = employee;
        this.status = status;
        this.startAt = startAt;
        this.endAt = endAt;
        this.createdAt = createdAt;
        this.comment = comment;
    }
}

