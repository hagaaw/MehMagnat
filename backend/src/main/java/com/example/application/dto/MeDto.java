package com.example.application.dto;

import com.example.application.entity.forEmployee.EmployeeRole;
import lombok.Data;

@Data
public class MeDto {
    private Long id;
    private String fullName;
    private EmployeeRole role;

    public MeDto(Long id, String fullName, EmployeeRole role) {
        this.id = id; this.fullName = fullName; this.role = role;
    }
}
