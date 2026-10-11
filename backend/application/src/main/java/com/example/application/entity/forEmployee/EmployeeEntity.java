package com.example.application.entity.forEmployee;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="employees")
@Getter
@Setter
@NoArgsConstructor
public class EmployeeEntity {
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

    @Column(name="role", nullable = false)
    @Enumerated(EnumType.STRING)
    private EmployeeRole role;

    @Column(name="is_active", nullable = false)
    private Boolean isActive;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    public EmployeeEntity(String fullName, String phone,
                          String email,
                          EmployeeRole role,
                          Boolean isActive) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = LocalDateTime.now();
    }
}
