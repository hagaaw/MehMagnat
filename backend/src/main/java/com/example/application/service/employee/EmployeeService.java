package com.example.application.service.employee;


import com.example.application.entity.forEmployee.EmployeeEntity;
import com.example.application.entity.forEmployee.EmployeeRole;
import com.example.application.repository.employee.EmployeeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
@Transactional
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
        if (employee.getIsActive() == null) {
            employee.setIsActive(true);
        }
        employeeRepository.save(employee);
        return true;
    }

    // Все сотрудники для страницы администратора (в контроллере превращаются в EmployeeDto без пароля)
    @Transactional(readOnly = true)
    public List<EmployeeEntity> findAll() {
        return employeeRepository.findAll();
    }

    public EmployeeEntity findByPhone(String phone){
        return employeeRepository.findByPhone(phone).orElseThrow(() -> new IllegalArgumentException("Такого сотрудника нет"));
    }

    public EmployeeEntity findByEmployeeId(Long salleId) {
        return employeeRepository.findById(salleId).orElseThrow(()-> new IllegalArgumentException("Такого продовца нет"));
    }

    public EmployeeEntity getMe() {
        return employeeRepository.findByPhone(SecurityContextHolder.getContext().getAuthentication().getName()).orElse(null);
    }

    public boolean roleChange(String phone, EmployeeRole role) {
        var employee = employeeRepository.findByPhone(phone).orElseThrow(()->new IllegalArgumentException("not found"));
        employee.setRole(role);
        employeeRepository.save(employee);
        return true;
    }

    public boolean setActive(String phone, boolean active) {
        var employee = employeeRepository.findByPhone(phone).orElseThrow(() -> new IllegalArgumentException("not found"));
        employee.setIsActive(active);
        employeeRepository.save(employee);
        return true;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean deleteEmployeeByPhone(String phone) {
        var employee = employeeRepository.findByPhone(phone).orElseThrow(() -> new IllegalArgumentException("not found"));
        try {
            employeeRepository.delete(employee);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }
}
