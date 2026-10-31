package com.aether.ms_auth.profile;

import com.aether.ms_auth.profile.dto.input.GetMyProfileInputDTO;
import com.aether.ms_auth.profile.dto.input.UpdateProfileInputDTO;
import com.aether.ms_auth.profile.dto.output.GetMyProfileOutputDTO;
import com.aether.ms_auth.profile.dto.output.UpdateProfileOutputDTO;
import com.aether.ms_auth.profile.services.ProfileService;
import com.aether.ms_auth.shared.enums.EmployeeStatusEnum;
import com.aether.ms_auth.shared.exceptions.NotFoundException;
import com.aether.ms_auth.shared.persistence.mongo.documents.NotificationConfigDocument;
import com.aether.ms_auth.shared.persistence.mongo.repository.NotificationConfigRepository;
import com.aether.ms_auth.shared.persistence.postgres.entities.*;
import com.aether.ms_auth.shared.persistence.postgres.repositories.EmployeeRepository;
import com.aether.ms_auth.shared.persistence.postgres.repositories.StorageFileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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
  private NotificationConfigRepository notificationConfigRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private ProfileService profileService;

  @Test
  @DisplayName("Should return an user and its permissions")
  void getMyProfile() {
    PermissionEntity permission = new PermissionEntity(
        "Permission Test",
        "A Permission Test",
        ".*"
    );

    PermissionGroupEntity permissionGroup = new PermissionGroupEntity(
        "Permission Group Test"
    );

    permissionGroup.setPermissions(List.of(permission));

    AddressEntity address = new AddressEntity(
        "89200-000",
        "SC",
        "Joinville",
        "Centro",
        "Rua Teste",
        123,
        "Apto 1"
    );

    EnterpriseEntity enterprise = new EnterpriseEntity(
        "teste",
        "teste S.A",
        "1234567890123",
        address
    );

    UnitEntity unit = new UnitEntity(
        "1234567",
        "12345678000190",
        enterprise,
        address
    );

    DepartmentEntity department = new DepartmentEntity(
        "1234567890123",
        "Department Test",
        unit
    );

    StorageFileEntity image = new StorageFileEntity(
        "profile.png",
        "/images/profile.png"
    );

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

    employee.setDepartment(department);
    employee.setStorageFile(image);

    GetMyProfileInputDTO input = new GetMyProfileInputDTO(
        employee.getId()
    );

    when(employeeRepository.findByIdAndStatus(
        employee.getId(),
        EmployeeStatusEnum.ACTIVE
    )).thenReturn(Optional.of(employee));

    GetMyProfileOutputDTO output = profileService.getMyProfile(input);

    assertEquals(employee.getId(), output.id());
    assertEquals("123.456.789-01", output.cpf());
    assertEquals("test@gmail.com", output.email());
    assertEquals("Test", output.name());
    assertEquals("(11) 9999-9999", output.phone());

    GetMyProfileOutputDTO.Permission expectedPermission =
        new GetMyProfileOutputDTO.Permission(
            permission.getId(),
            permission.getName(),
            permission.getDescription()
        );

    assertEquals(List.of(expectedPermission), output.permissions());

    assertEquals(image.getId(), output.image().id());
    assertEquals(image.getName(), output.image().name());
    assertEquals(image.getPath(), output.image().path());

    assertEquals(unit.getId(), output.unit().id());
    assertEquals(unit.getCnae(), output.unit().cnae());
    assertEquals(unit.getCnpj(), output.unit().cnpj());
    assertEquals(unit.getIsActive(), output.unit().isActive());

    assertEquals(address.getId(), output.unit().address().id());
    assertEquals(address.getZipCode(), output.unit().address().zipCode());
    assertEquals(address.getState(), output.unit().address().state());
    assertEquals(address.getCity(), output.unit().address().city());
    assertEquals(address.getNeighborhood(), output.unit().address().neighborhood());
    assertEquals(address.getStreet(), output.unit().address().street());
    assertEquals(address.getNumber(), output.unit().address().number());
    assertEquals(address.getComplement(), output.unit().address().complement());

    assertEquals(department.getId(), output.department().id());
    assertEquals(department.getName(), output.department().name());
    assertEquals(department.getDescription(), output.department().description());
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
        employee.getId(), "Nome Novo", "1188888888", null, null, null, null);

    when(employeeRepository.findByIdAndStatus(employee.getId(), EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.of(employee));
    when(employeeRepository.save(employee)).thenReturn(employee);

    UpdateProfileOutputDTO output = profileService.updateUser(input);

    assertEquals("Nome Novo", employee.getName());
    assertEquals("1188888888", employee.getPhone());
  }

  @Test
  @DisplayName("Should update phone when current phone is null")
  void updateUserPhoneWhenCurrentPhoneIsNull(){
    EmployeeEntity employee = new EmployeeEntity(
        "12345678901", "nome", "test@gmail.com", "senhaHash", null,
        EmployeeStatusEnum.ACTIVE, null
    );

    UpdateProfileInputDTO input = new UpdateProfileInputDTO(
        employee.getId(), null, "1188888888", null, null, null, null);

    when(employeeRepository.findByIdAndStatus(employee.getId(), EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.of(employee));
    when(employeeRepository.save(employee)).thenReturn(employee);

    profileService.updateUser(input);

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
        employee.getId(), null, null, "novaSenha", null, null, null);

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
        employee.getId(), null, null, null, null, null, image);

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
        employee.getId(), null, null, null, null, null, null);

    when(employeeRepository.findByIdAndStatus(employee.getId(), EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.of(employee));
    when(employeeRepository.save(employee)).thenReturn(employee);

    profileService.updateUser(input);

    assertEquals("nome", employee.getName());
    assertEquals("1199999999", employee.getPhone());
    verify(storageFileRepository, org.mockito.Mockito.never()).save(any());
    verify(notificationConfigRepository, org.mockito.Mockito.never()).save(any());
  }

  private EmployeeEntity activeEmployee(){
    EmployeeEntity employee = new EmployeeEntity(
        "12345678901", "nome", "test@gmail.com", "senhaHash", "1199999999",
        EmployeeStatusEnum.ACTIVE, null
    );
    when(employeeRepository.findByIdAndStatus(employee.getId(), EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.of(employee));
    return employee;
  }

  private UpdateProfileInputDTO tokenInput(EmployeeEntity employee, String token, UpdateProfileInputDTO.Notifications notifications){
    return new UpdateProfileInputDTO(employee.getId(), null, null, null, token, notifications, null);
  }

  private NotificationConfigDocument savedConfig(){
    ArgumentCaptor<NotificationConfigDocument> captor = ArgumentCaptor.forClass(NotificationConfigDocument.class);
    verify(notificationConfigRepository, atLeastOnce()).save(captor.capture());
    return captor.getValue();
  }

  private NotificationConfigDocument configWithTokens(Integer userId, String... tokens){
    NotificationConfigDocument config = new NotificationConfigDocument(userId);
    for (String token : tokens) {
      config.getTokens().add(new NotificationConfigDocument.Token(token, Instant.EPOCH, Instant.EPOCH));
    }
    return config;
  }

  @Test
  @DisplayName("Should persist a valid device token")
  void updateUserValidDeviceToken(){
    EmployeeEntity employee = activeEmployee();
    when(notificationConfigRepository.findByUserId(employee.getId())).thenReturn(Optional.empty());

    profileService.updateUser(tokenInput(employee, "token-A", null));

    verify(notificationConfigRepository).save(any(NotificationConfigDocument.class));

    NotificationConfigDocument document = savedConfig();
    assertEquals(employee.getId(), document.getUserId());
    assertEquals(1, document.getTokens().size());
    assertEquals("token-A", document.getTokens().get(0).getToken());
    assertNotNull(document.getTokens().get(0).getCreatedAt());
  }

  @Test
  @DisplayName("Should not duplicate an existing token and should refresh its last use on every login")
  void updateUserExistingDeviceToken(){
    EmployeeEntity employee = activeEmployee();
    NotificationConfigDocument existing = configWithTokens(employee.getId(), "token-A");
    when(notificationConfigRepository.findByUserId(employee.getId())).thenReturn(Optional.of(existing));

    profileService.updateUser(tokenInput(employee, "token-A", null));
    profileService.updateUser(tokenInput(employee, "token-A", null));

    assertEquals(1, existing.getTokens().size());
    assertTrue(existing.getTokens().get(0).getLastUsedAt().isAfter(Instant.EPOCH));
    assertEquals(Instant.EPOCH, existing.getTokens().get(0).getCreatedAt());
  }

  @Test
  @DisplayName("Should keep multiple device tokens for the same user")
  void updateUserMultipleDevices(){
    EmployeeEntity employee = activeEmployee();
    NotificationConfigDocument existing = configWithTokens(employee.getId(), "token-A");
    when(notificationConfigRepository.findByUserId(employee.getId())).thenReturn(Optional.of(existing));

    profileService.updateUser(tokenInput(employee, "token-B", null));

    assertEquals(
        List.of("token-A", "token-B"),
        existing.getTokens().stream().map(NotificationConfigDocument.Token::getToken).toList()
    );
  }

  @Test
  @DisplayName("Should move a token owned by another user to the authenticated user")
  void updateUserTokenOwnedByAnotherUser(){
    EmployeeEntity employee = activeEmployee();
    when(notificationConfigRepository.findByUserId(employee.getId())).thenReturn(Optional.empty());

    NotificationConfigDocument other = configWithTokens(99, "token-A");
    when(notificationConfigRepository.findByTokensToken("token-A")).thenReturn(List.of(other));

    profileService.updateUser(tokenInput(employee, "token-A", null));

    assertTrue(other.getTokens().isEmpty());
    verify(notificationConfigRepository).save(other);
    assertEquals("token-A", savedConfig().getTokens().get(0).getToken());
  }

  @Test
  @DisplayName("Should update only notification preferences without a token")
  void updateUserOnlyNotifications(){
    EmployeeEntity employee = activeEmployee();
    NotificationConfigDocument existing = configWithTokens(employee.getId(), "token-A");
    when(notificationConfigRepository.findByUserId(employee.getId())).thenReturn(Optional.of(existing));

    profileService.updateUser(tokenInput(employee, null, new UpdateProfileInputDTO.Notifications(false, true)));

    assertFalse(existing.getNotify().getEmail());
    assertTrue(existing.getNotify().getMobile());
    assertEquals(1, existing.getTokens().size());
  }

  @Test
  @DisplayName("A token must not change notification preferences")
  void updateUserTokenDoesNotChangePreferences(){
    EmployeeEntity employee = activeEmployee();
    NotificationConfigDocument existing = new NotificationConfigDocument(employee.getId());
    existing.getNotify().setMobile(false);
    when(notificationConfigRepository.findByUserId(employee.getId())).thenReturn(Optional.of(existing));

    profileService.updateUser(tokenInput(employee, "token-A", null));

    assertFalse(existing.getNotify().getMobile());
    assertNull(existing.getNotify().getEmail());
  }

  @Test
  @DisplayName("Should throw an exception if does not find the user")
  void updateUserNotFound(){
    UpdateProfileInputDTO input = new UpdateProfileInputDTO(999, "Nome", null, null, null, null, null);

    when(employeeRepository.findByIdAndStatus(999, EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> profileService.updateUser(input));
  }
}
