package com.aether.ms_auth.profile;

import com.aether.ms_auth.profile.dto.input.GetMyProfileInputDTO;
import com.aether.ms_auth.profile.dto.output.GetMyProfileInfosOutputDTO;
import com.aether.ms_auth.profile.dto.output.GetMyProfileOutputDTO;
import com.aether.ms_auth.profile.services.ProfileService;
import com.aether.ms_auth.shared.enums.EmployeeStatusEnum;
import com.aether.ms_auth.shared.exceptions.NotFoundException;
import com.aether.ms_auth.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_auth.shared.persistence.postgres.entities.PermissionEntity;
import com.aether.ms_auth.shared.persistence.postgres.entities.PermissionGroupEntity;
import com.aether.ms_auth.shared.persistence.postgres.repositories.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService Tests")
public class ProfileServiceTests {
  @Mock
  private EmployeeRepository employeeRepository;

  @InjectMocks
  private ProfileService profileService;

  @Test
  @DisplayName("Should return an user and its permissions")
  void getMyProfile(){
    PermissionEntity permission = new PermissionEntity(
        "Permission Test",
        "A Permission Test"
    );

    GetMyProfileInfosOutputDTO.Permission expectedPermission =
        new GetMyProfileInfosOutputDTO.Permission(permission);

    PermissionGroupEntity permissionGroup = new PermissionGroupEntity(
        "Permission Group Test"
    );

    permissionGroup.setPermissions(List.of(permission));

    String cpf = "123.456.789-01";
    String name = "Test";

    EmployeeEntity employee = new EmployeeEntity(
      cpf.replaceAll("[^\\d]", ""),
      name.toLowerCase(),
      "test@gmail.com",
      "1199999999",
      EmployeeStatusEnum.ACTIVE,
      List.of(
          permissionGroup
      )
    );

    GetMyProfileInputDTO input = new GetMyProfileInputDTO(
        employee.getId()
    );

    when(employeeRepository.findByIdAndStatus(employee.getId(), EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.of(employee));

    GetMyProfileOutputDTO output = profileService.getMyProfile(input);

    assertEquals(output.cpf(), cpf);
    assertEquals(output.name(), name);

    assertEquals(output.permissions(), List.of(expectedPermission));
  }

  @Test
  @DisplayName("Should throw an exception if does not found my user")
  void getMyProfileNotFound(){
    String cpf = "123.456.789-01";
    String name = "Test";

    EmployeeEntity employee = new EmployeeEntity(
        cpf.replaceAll("[^\\d]", ""),
        name.toLowerCase(),
        "test@gmail.com",
        "1199999999",
        EmployeeStatusEnum.ACTIVE,
        new ArrayList<>()
    );

    GetMyProfileInputDTO input = new GetMyProfileInputDTO(
        employee.getId()
    );

    when(employeeRepository.findByIdAndStatus(employee.getId(), EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> profileService.getMyProfile(input));
  }
}
