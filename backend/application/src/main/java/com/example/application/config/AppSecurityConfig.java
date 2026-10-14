package com.example.application.config;


import com.example.application.entity.forEmployee.EmployeeRole;
import com.example.application.repository.employee.EmployeeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
public class AppSecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http){
        return http
                .csrf(CsrfConfigurer::spa)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/products/**").hasAnyRole(EmployeeRole.ADMIN.name(), EmployeeRole.SALER.name())
                        .requestMatchers("/api/fur-type/**").hasAnyRole(EmployeeRole.ADMIN.name(), EmployeeRole.SALER.name())
                        .requestMatchers("/api/reservation/**").hasAnyRole(EmployeeRole.ADMIN.name(), EmployeeRole.SALER.name())
                        .requestMatchers("/api/registration/**").hasRole(EmployeeRole.ADMIN.name())
                        .anyRequest().permitAll())
                .formLogin(login -> login
                        .loginProcessingUrl("/login")
                        .usernameParameter("phone")
                        .passwordParameter("password")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .permitAll())
                .build();
    }
    @Bean
    public UserDetailsService userDetailsService(EmployeeRepository employeeRepository){
        return (phone) -> {
            var employee = employeeRepository.findByPhone(phone).orElseThrow(() -> new UsernameNotFoundException("not found"));
            return User.builder()
                    .username(phone)
                    .password(employee.getPassword())
                    .roles(employee.getRole().name())
                    .build();
        };
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
