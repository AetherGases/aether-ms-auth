package com.aether.ms_auth.profile;

import com.aether.ms_auth.profile.dto.input.GetMyProfileInputDTO;
import com.aether.ms_auth.profile.dto.input.UpdateProfileInputDTO;
import com.aether.ms_auth.profile.dto.output.GetMyProfileInfosOutputDTO;
import com.aether.ms_auth.profile.dto.output.GetMyProfileOutputDTO;
import com.aether.ms_auth.profile.dto.output.UpdateProfileOutputDTO;
import com.aether.ms_auth.profile.services.ProfileService;
import com.aether.ms_auth.shared.enums.EmployeeStatusEnum;
import com.aether.ms_auth.shared.exceptions.NotFoundException;
import com.aether.ms_auth.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_auth.shared.persistence.postgres.entities.PermissionEntity;
import com.aether.ms_auth.shared.persistence.postgres.entities.PermissionGroupEntity;
import com.aether.ms_auth.shared.persistence.postgres.entities.StorageFileEntity;
import com.aether.ms_auth.shared.persistence.postgres.repositories.EmployeeRepository;
import com.aether.ms_auth.shared.persistence.postgres.repositories.StorageFileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService Tests")
public class ProfileServiceTests {
  @Mock
  private EmployeeRepository employeeRepository;

  @Mock
  private StorageFileRepository storageFileRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

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
      permissionGroup
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
        null
    );

    GetMyProfileInputDTO input = new GetMyProfileInputDTO(
        employee.getId()
    );

    when(employeeRepository.findByIdAndStatus(employee.getId(), EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> profileService.getMyProfile(input));
  }

  @Test
  @DisplayName("Should update name and phone")
  void updateUserNameAndPhone(){
    EmployeeEntity employee = new EmployeeEntity(
        "12345678901", "nome antigo", "test@gmail.com", "senhaHash", "1199999999",
        EmployeeStatusEnum.ACTIVE, null
    );

    UpdateProfileInputDTO input = new UpdateProfileInputDTO(
        employee.getId(), "Nome Novo", "1188888888", null, null
    );

    when(employeeRepository.findByIdAndStatus(employee.getId(), EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.of(employee));
    when(employeeRepository.save(employee)).thenReturn(employee);

    UpdateProfileOutputDTO output = profileService.updateUser(input);

    assertEquals("Nome Novo", employee.getName());
    assertEquals("1188888888", employee.getPhone());
  }

  @Test
  @DisplayName("Should update password hash")
  void updateUserPassword(){
    EmployeeEntity employee = new EmployeeEntity(
        "12345678901", "nome", "test@gmail.com", "senhaAntigaHash", "1199999999",
        EmployeeStatusEnum.ACTIVE, null
    );

    UpdateProfileInputDTO input = new UpdateProfileInputDTO(
        employee.getId(), null, null, "novaSenha", null
    );

    when(employeeRepository.findByIdAndStatus(employee.getId(), EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.of(employee));
    when(passwordEncoder.encode("novaSenha")).thenReturn("novaSenhaHash");
    when(employeeRepository.save(employee)).thenReturn(employee);

    profileService.updateUser(input);

    assertEquals("novaSenhaHash", employee.getPasswordHash());
  }

  @Test
  @DisplayName("Should update storage file image")
  void updateUserImage(){
    StorageFileEntity storageFile = new StorageFileEntity();
    EmployeeEntity employee = new EmployeeEntity(
        "12345678901", "nome", "test@gmail.com", "senhaHash", "1199999999",
        EmployeeStatusEnum.ACTIVE, null
    );
    employee.setStorageFile(storageFile);

    UpdateProfileInputDTO.Image image = new UpdateProfileInputDTO.Image("foto.png", "http://cdn/foto.png");
    UpdateProfileInputDTO input = new UpdateProfileInputDTO(
        employee.getId(), null, null, null, image
    );

    when(employeeRepository.findByIdAndStatus(employee.getId(), EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.of(employee));
    when(employeeRepository.save(employee)).thenReturn(employee);

    profileService.updateUser(input);

    assertEquals("foto.png", storageFile.getName());
    assertEquals("http://cdn/foto.png", storageFile.getPath());
    verify(storageFileRepository).save(storageFile);
  }

  @Test
  @DisplayName("Should not change fields when input values are null")
  void updateUserNoChanges(){
    EmployeeEntity employee = new EmployeeEntity(
        "12345678901", "nome", "test@gmail.com", "senhaHash", "1199999999",
        EmployeeStatusEnum.ACTIVE, null
    );

    UpdateProfileInputDTO input = new UpdateProfileInputDTO(
        employee.getId(), null, null, null, null
    );

    when(employeeRepository.findByIdAndStatus(employee.getId(), EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.of(employee));
    when(employeeRepository.save(employee)).thenReturn(employee);

    profileService.updateUser(input);

    assertEquals("nome", employee.getName());
    assertEquals("1199999999", employee.getPhone());
    verify(storageFileRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  @DisplayName("Should throw an exception if does not find the user")
  void updateUserNotFound(){
    UpdateProfileInputDTO input = new UpdateProfileInputDTO(999, "Nome", null, null, null);

    when(employeeRepository.findByIdAndStatus(999, EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> profileService.updateUser(input));
  }
}
