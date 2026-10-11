package com.aether.ms_auth.shared.security;

import com.aether.ms_auth.shared.enums.EmployeeStatusEnum;
import com.aether.ms_auth.shared.exceptions.UnauthorizedException;
import com.aether.ms_auth.shared.helpers.CustomUserDetails;
import com.aether.ms_auth.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_auth.shared.persistence.postgres.entities.PermissionEntity;
import com.aether.ms_auth.shared.persistence.postgres.entities.PermissionGroupEntity;
import com.aether.ms_auth.shared.persistence.postgres.repositories.EmployeeRepository;
import com.aether.ms_auth.shared.services.MessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {
  private static final String EMAIL = "employee@example.com";

  @Mock private EmployeeRepository employeeRepository;
  @Mock private MessageService messageService;
  @InjectMocks private UserService userService;

  @Test
  void loadsActiveEmployeeWithCurrentPermissions() {
    PermissionEntity permission = new PermissionEntity();
    permission.setName("Gerenciar funcionários");
    PermissionGroupEntity group = new PermissionGroupEntity("Management");
    group.setPermissions(List.of(permission));
    EmployeeEntity employee = new EmployeeEntity();
    employee.setId(42);
    employee.setEmail(EMAIL);
    employee.setPasswordHash("test-password-hash");
    employee.setStatus(EmployeeStatusEnum.ACTIVE);
    employee.setPermissionGroup(group);
    when(employeeRepository.findByEmailAndStatus(EMAIL, EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.of(employee));

    var result = userService.loadUserByUsername(EMAIL);

    assertEquals(EMAIL, result.getUsername());
    assertEquals(42, ((CustomUserDetails) result).getId());
    assertEquals(List.of("Gerenciar funcionários"),
        result.getAuthorities().stream().map(authority -> authority.getAuthority()).toList());
    verify(employeeRepository).findByEmailAndStatus(EMAIL, EmployeeStatusEnum.ACTIVE);
    verify(employeeRepository, never()).findByEmail(anyString());
  }

  @Test
  void rejectsWhenNoActiveEmployeeMatchesEmail() {
    when(employeeRepository.findByEmailAndStatus(EMAIL, EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.empty());
    when(messageService.getMessage("exception.login.invalid")).thenReturn("Invalid login");

    assertThrows(UnauthorizedException.class, () -> userService.loadUserByUsername(EMAIL));
    verify(employeeRepository).findByEmailAndStatus(EMAIL, EmployeeStatusEnum.ACTIVE);
    verify(employeeRepository, never()).findByEmail(anyString());
  }
}