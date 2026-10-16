package com.example.application.entity.forCustomer;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="customers")
@Getter
@Setter
@NoArgsConstructor
public class CustomerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="full_name",nullable = false)
    private String fullName;

    @Column(name="phone", nullable = false, unique = true)
    private String phone;

    @Column(name="email", nullable = false, unique = true)
    private String email;
}
