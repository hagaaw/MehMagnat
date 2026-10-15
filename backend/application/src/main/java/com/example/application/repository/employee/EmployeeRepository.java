package com.example.application.repository.employee;

import com.example.application.entity.forEmployee.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {

    Optional<EmployeeEntity> findByPhone(String phone);

    Optional<EmployeeEntity> findById(Long id);
}
