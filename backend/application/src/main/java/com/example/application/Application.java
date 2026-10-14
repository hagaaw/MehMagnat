package com.example.application;

import com.example.application.entity.forEmployee.EmployeeEntity;
import com.example.application.entity.forEmployee.EmployeeRole;
import com.example.application.repository.employee.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

@SpringBootApplication
@EnableScheduling
public class Application implements CommandLineRunner {

	private final BCryptPasswordEncoder passwordEncoder;
	private final EmployeeRepository employeeRepository;

	public Application(BCryptPasswordEncoder passwordEncoder, EmployeeRepository employeeRepository) {
		this.passwordEncoder = passwordEncoder;
		this.employeeRepository = employeeRepository;
	}

	@Value("${app.admin.password}")
	private String password;

	@Value("${app.admin.phone}")
	private String phone;

	@Value("${app.admin.email}")
	private String email;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		if (employeeRepository.findByPhone(phone).isEmpty()) {
			var admin = new EmployeeEntity();
			admin.setPhone(phone);
			admin.setPassword(passwordEncoder.encode(password));
			admin.setRole(EmployeeRole.ADMIN);
			admin.setCreatedAt(LocalDateTime.now());
			admin.setEmail(email);
			admin.setIsActive(true);
			admin.setFullName("Илья Яхнис");
			employeeRepository.save(admin);
		}
	}
}
