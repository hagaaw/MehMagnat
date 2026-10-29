package com.example.application.controller.registration;

import com.example.application.entity.forEmployee.EmployeeEntity;
import com.example.application.service.employee.EmployeeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name="registration")
@RestController
@RequestMapping("/api/registration")
public class RegistrationController {
    private final EmployeeService employeeService;
    private Logger logger = LoggerFactory.getLogger(RegistrationController.class);

    public RegistrationController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    public ResponseEntity<Boolean> registrationEmployee(@RequestBody EmployeeEntity employee) {
        logger.info("Вызван метод добавления работника");
        var isSaved = employeeService.saveEmployee(employee);
        return isSaved
                ? ResponseEntity.ok(true)
                : ResponseEntity.status(400).body(false);
    }




}
