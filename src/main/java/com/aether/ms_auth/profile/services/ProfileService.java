package com.aether.ms_auth.profile.services;

import com.aether.ms_auth.profile.dto.input.GetMyProfileInputDTO;
import com.aether.ms_auth.profile.dto.input.UpdateProfileInputDTO;
import com.aether.ms_auth.profile.dto.output.GetMyProfileOutputDTO;
import com.aether.ms_auth.profile.dto.output.UpdateProfileOutputDTO;
import com.aether.ms_auth.profile.mappers.ProfileMapper;
import com.aether.ms_auth.shared.enums.EmployeeStatusEnum;
import com.aether.ms_auth.shared.exceptions.NotFoundException;
import com.aether.ms_auth.shared.persistence.mongo.documents.NotificationConfigDocument;
import com.aether.ms_auth.shared.persistence.mongo.repository.NotificationConfigRepository;
import com.aether.ms_auth.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_auth.shared.persistence.postgres.entities.StorageFileEntity;
import com.aether.ms_auth.shared.persistence.postgres.repositories.EmployeeRepository;
import com.aether.ms_auth.shared.persistence.postgres.repositories.StorageFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProfileService {
  private final EmployeeRepository employeeRepository;
  private final StorageFileRepository storageFileRepository;
  private final NotificationConfigRepository notificationConfigRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  public GetMyProfileOutputDTO getMyProfile(GetMyProfileInputDTO input){
    EmployeeEntity employee = employeeRepository.findByIdAndStatus(input.id(), EmployeeStatusEnum.ACTIVE).orElseThrow(
        () -> new NotFoundException("exception.profile.not-found")
    );

    return ProfileMapper.convertEntityToGetProfileOutput(employee, findNotify(employee.getId()));
  }

  @Transactional
  public UpdateProfileOutputDTO updateUser(UpdateProfileInputDTO input){
    EmployeeEntity employee = employeeRepository.findByIdAndStatus(input.id(), EmployeeStatusEnum.ACTIVE).orElseThrow(
        () -> new NotFoundException("exception.profile.not-found")
    );

    String deviceToken = input.deviceToken() != null && !input.deviceToken().isBlank() ? input.deviceToken() : null;

    if (input.name() != null && !employee.getName().equals(input.name())) employee.setName(input.name());
    if (input.phone() != null && !input.phone().equals(employee.getPhone())) employee.setPhone(input.phone());
    if (input.newPassword() != null) employee.setPasswordHash(passwordEncoder.encode(input.newPassword()));
    if (input.image() != null){
      StorageFileEntity storageFile = employee.getStorageFile();

      if (storageFile == null) {
        storageFile = new StorageFileEntity();
      }

      storageFile.setName(input.image().imageName());
      storageFile.setPath(input.image().imageUrl());

      storageFileRepository.save(storageFile);
      employee.setStorageFile(storageFile);
    }

    employeeRepository.save(employee);

    NotificationConfigDocument.Notify notify;
    if (deviceToken != null || input.notifications() != null) {
      notify = updateNotificationConfig(employee.getId(), input.notifications(), deviceToken);
    } else {
      notify = findNotify(employee.getId());
    }

    return ProfileMapper.convertEntityToUpdateProfileOutput(employee, notify);
  }

  private NotificationConfigDocument.Notify findNotify(Integer userId){
    return notificationConfigRepository.findByUserId(userId)
        .map(NotificationConfigDocument::getNotify)
        .orElseGet(NotificationConfigDocument.Notify::new);
  }

  private NotificationConfigDocument.Notify updateNotificationConfig(
      Integer userId,
      UpdateProfileInputDTO.Notifications notifications,
      String deviceToken
  ){
    NotificationConfigDocument config = notificationConfigRepository.findByUserId(userId)
        .orElseGet(() -> new NotificationConfigDocument(userId));

    if (notifications != null) {
      if (notifications.email() != null) config.getNotify().setEmail(notifications.email());
      if (notifications.mobile() != null) config.getNotify().setMobile(notifications.mobile());
    }

    if (deviceToken != null) {
      Instant now = Instant.now();

      notificationConfigRepository.findByTokensToken(deviceToken).stream()
          .filter(other -> !Objects.equals(userId, other.getUserId()))
          .forEach(other -> {
            other.getTokens().removeIf(t -> deviceToken.equals(t.getToken()));
            notificationConfigRepository.save(other);
          });

      config.getTokens().stream()
          .filter(t -> deviceToken.equals(t.getToken()))
          .findFirst()
          .ifPresentOrElse(
              t -> t.setLastUsedAt(now),
              () -> config.getTokens().add(new NotificationConfigDocument.Token(deviceToken, now, now))
          );
    }

    notificationConfigRepository.save(config);
    return config.getNotify();
  }
}
