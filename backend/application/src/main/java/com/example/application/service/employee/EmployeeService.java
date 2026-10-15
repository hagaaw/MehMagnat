package com.example.application.service.employee;


import com.example.application.entity.forEmployee.EmployeeEntity;
import com.example.application.repository.employee.EmployeeRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository, BCryptPasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean saveEmployee(EmployeeEntity employee){

        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        employee.setCreatedAt(LocalDateTime.now());
        employeeRepository.save(employee);
        return true;
    }

    public EmployeeEntity findByPhone(String phone){
        return employeeRepository.findByPhone(phone).orElseThrow(() -> new IllegalArgumentException("Такого сотрудника нет"));
    }

    public EmployeeEntity findByEmployeeId(Long salleId) {
        return employeeRepository.findById(salleId).orElseThrow(()-> new IllegalArgumentException("Такого продовца нет"));
    }
}
