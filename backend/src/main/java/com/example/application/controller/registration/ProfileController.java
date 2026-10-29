package com.example.application.controller.registration;


import com.example.application.dto.MeDto;
import com.example.application.entity.forEmployee.EmployeeEntity;
import com.example.application.service.employee.EmployeeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name="profile")
@RestController
@RequestMapping("/api/me")
public class ProfileController {
    private final EmployeeService employeeService;

    public ProfileController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public ResponseEntity<MeDto> getEmployee(){
        EmployeeEntity employee = employeeService.getMe();
        var me =  new MeDto(employee.getId(), employee.getFullName(), employee.getRole());
        if (me != null){
            return ResponseEntity.ok(me);
        }
        return ResponseEntity.status(400).build();

    }
}
