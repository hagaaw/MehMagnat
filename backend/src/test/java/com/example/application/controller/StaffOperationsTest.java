package com.example.application.controller;

import com.example.application.config.AppSecurityConfig;
import com.example.application.controller.registration.*;
import com.example.application.entity.forEmployee.*;
import com.example.application.repository.employee.EmployeeRepository;
import com.example.application.service.employee.EmployeeService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StaffOperationsTest {
    final EmployeeRepository repo = mock(EmployeeRepository.class);
    final AppSecurityConfig config = new AppSecurityConfig();
    final EmployeeService staff = new EmployeeService(repo, config.passwordEncoder());
    final RegistrationController controller = new RegistrationController(staff);
    final EmployeeEntity employee = new EmployeeEntity("Иван","123","ivan@example.org",EmployeeRole.SALER,null);
    final TestingAuthenticationToken admin = new TestingAuthenticationToken("admin","unused");

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void registersEmployeeWithEncodedPasswordAndDefaultActiveState() {
        employee.setPassword("sample");
        assertEquals(Boolean.TRUE,controller.registrationEmployee(employee).getBody());
        assertTrue(config.passwordEncoder().matches("sample",employee.getPassword()));
        assertTrue(employee.getIsActive());
        assertNotNull(employee.getCreatedAt());
        verify(repo).save(employee);
    }

    @Test void registrationPreservesExplicitInactiveState() {
        employee.setPassword("sample");
        employee.setIsActive(false);
        staff.saveEmployee(employee);
        assertFalse(employee.getIsActive());
    }

    @Test void listsEmployeeContactAndRole() {
        when(repo.findAll()).thenReturn(List.of(employee));
        var dto = controller.getAllEmployees().getBody().getFirst();
        assertEquals("123",dto.phone());
        assertEquals("Иван",dto.fullName());
        assertEquals(EmployeeRole.SALER,dto.role());
        assertEquals("ivan@example.org",dto.email());
    }

    @Test void profileUsesAuthenticatedEmployee() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("123","unused"));
        when(repo.findByPhone("123")).thenReturn(Optional.of(employee));
        var me = new ProfileController(staff).getEmployee().getBody();
        assertEquals("Иван",me.getFullName());
        assertEquals(EmployeeRole.SALER,me.getRole());
    }

    @Test void refusesChangesToOwnAccount() {
        assertEquals(400,controller.roleChange("admin",EmployeeRole.SALER,admin).getStatusCode().value());
        assertEquals(400,controller.setActive("admin",false,admin).getStatusCode().value());
        assertEquals(400,controller.deleteEmployee("admin",admin).getStatusCode().value());
        verifyNoInteractions(repo);
    }

    @Test void changesRoleAndActivityOfSelectedEmployee() {
        when(repo.findByPhone("123")).thenReturn(Optional.of(employee));
        assertEquals(Boolean.TRUE,controller.roleChange("123",EmployeeRole.ADMIN,admin).getBody());
        assertEquals(EmployeeRole.ADMIN,employee.getRole());
        assertEquals(Boolean.TRUE,controller.setActive("123",false,admin).getBody());
        assertFalse(employee.getIsActive());
        verify(repo,times(2)).save(employee);
    }

    @ParameterizedTest @ValueSource(booleans = {true,false})
    void deletionReportsRepositoryOutcome(boolean linked) {
        when(repo.findByPhone("123")).thenReturn(Optional.of(employee));
        if (linked) doThrow(new DataIntegrityViolationException("linked")).when(repo).delete(employee);
        var result = controller.deleteEmployee("123",admin);
        assertEquals(linked ? 400 : 200,result.getStatusCode().value());
        assertEquals(!linked,result.getBody());
        verify(repo).delete(employee);
    }

    @ParameterizedTest @ValueSource(booleans = {true,false})
    void accountDetailsRespectActivityAndRole(boolean active) {
        employee.setPassword("encoded");
        employee.setIsActive(active);
        when(repo.findByPhone("123")).thenReturn(Optional.of(employee));
        var details = config.userDetailsService(repo).loadUserByUsername("123");
        assertEquals(active,details.isEnabled());
        assertEquals("123",details.getUsername());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SALER")));
    }

    @Test void cannotLoadMissingEmployee() {
        assertThrows(UsernameNotFoundException.class, () -> config.userDetailsService(repo).loadUserByUsername("none"));
        assertThrows(IllegalArgumentException.class, () -> staff.findByPhone("none"));
        assertThrows(IllegalArgumentException.class, () -> staff.findByEmployeeId(42L));
    }
}
