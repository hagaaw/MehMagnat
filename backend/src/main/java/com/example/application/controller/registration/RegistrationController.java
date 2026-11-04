package com.example.application.controller.registration;

import com.example.application.dto.EmployeeDto;
import com.example.application.entity.forEmployee.EmployeeEntity;
import com.example.application.entity.forEmployee.EmployeeRole;
import com.example.application.service.employee.EmployeeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@Tag(name = "registration")
@RestController
@RequestMapping("/api/registration")
public class RegistrationController {
    private final EmployeeService employeeService;
    private final Logger logger = LoggerFactory.getLogger(RegistrationController.class);

    public RegistrationController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public ResponseEntity<List<EmployeeDto>> getAllEmployees() {

        var employees = employeeService.findAll().stream().map(EmployeeDto::from).toList();
        return ResponseEntity.ok(employees);
    }

    @PostMapping
    public ResponseEntity<Boolean> registrationEmployee(@RequestBody EmployeeEntity employee) {
        logger.info("Вызван метод добавления работника");
        var isSaved = employeeService.saveEmployee(employee);
        return isSaved
                ? ResponseEntity.ok(true)
                : ResponseEntity.status(400).body(false);
    }

    @PutMapping("/role")
    public ResponseEntity<?> roleChange(
            @RequestParam String phone,
            @RequestParam EmployeeRole role,
            Authentication authentication
    ) {
        if (isSelf(authentication, phone)) {
            return selfError("Нельзя изменить роль самому себе.");
        }
        return employeeService.roleChange(phone, role)
                ? ResponseEntity.ok(true)
                : ResponseEntity.status(400).body(false);
    }

    @PutMapping("/active")
    public ResponseEntity<?> setActive(
            @RequestParam String phone,
            @RequestParam boolean active,
            Authentication authentication
    ) {
        if (isSelf(authentication, phone)) {
            return selfError("Нельзя отключить собственную учётную запись.");
        }
        return employeeService.setActive(phone, active)
                ? ResponseEntity.ok(true)
                : ResponseEntity.status(400).body(false);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteEmployee(
            @RequestParam String phone,
            Authentication authentication
    ) {
        if (isSelf(authentication, phone)) {
            return selfError("Нельзя удалить собственную учётную запись.");
        }
        return employeeService.deleteEmployeeByPhone(phone)
                ? ResponseEntity.ok(true)
                : ResponseEntity.status(400).body(false);
    }


    private boolean isSelf(Authentication authentication, String phone) {
        return authentication != null && phone.equals(authentication.getName());
    }


    private ResponseEntity<Map<String, String>> selfError(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
