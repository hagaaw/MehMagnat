package com.example.application.dto;

import com.example.application.entity.forEmployee.EmployeeEntity;
import com.example.application.entity.forEmployee.EmployeeRole;

import java.time.LocalDateTime;

public record EmployeeDto(
        Long id,
        String fullName,
        String phone,
        String email,
        EmployeeRole role,
        Boolean isActive,
        LocalDateTime createdAt
) {
    public static EmployeeDto from(EmployeeEntity e) {
        return new EmployeeDto(
                e.getId(),
                e.getFullName(),
                e.getPhone(),
                e.getEmail(),
                e.getRole(),
                e.getIsActive(),
                e.getCreatedAt()
        );
    }
}
