package com.aether.ms_auth.profile.services;

import com.aether.ms_auth.profile.dto.input.GetMyProfileInputDTO;
import com.aether.ms_auth.profile.dto.input.UpdateProfileInputDTO;
import com.aether.ms_auth.profile.dto.output.GetMyProfileOutputDTO;
import com.aether.ms_auth.profile.dto.output.UpdateProfileOutputDTO;
import com.aether.ms_auth.profile.mappers.ProfileMapper;
import com.aether.ms_auth.shared.enums.EmployeeStatusEnum;
import com.aether.ms_auth.shared.exceptions.NotFoundException;
import com.aether.ms_auth.shared.persistence.mongo.documents.DeviceTokenDocument;
import com.aether.ms_auth.shared.persistence.mongo.repository.DeviceTokenRepository;
import com.aether.ms_auth.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_auth.shared.persistence.postgres.entities.StorageFileEntity;
import com.aether.ms_auth.shared.persistence.postgres.repositories.EmployeeRepository;
import com.aether.ms_auth.shared.persistence.postgres.repositories.StorageFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {
  private final EmployeeRepository employeeRepository;
  private final StorageFileRepository storageFileRepository;
  private final DeviceTokenRepository deviceTokenRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  public GetMyProfileOutputDTO getMyProfile(GetMyProfileInputDTO input){
    EmployeeEntity employee = employeeRepository.findByIdAndStatus(input.id(), EmployeeStatusEnum.ACTIVE).orElseThrow(
        () -> new NotFoundException("exception.profile.not-found")
    );

    return ProfileMapper.convertEntityToGetProfileOutput(employee);
  }

  @Transactional
  public UpdateProfileOutputDTO updateUser(UpdateProfileInputDTO input){
    EmployeeEntity employee = employeeRepository.findByIdAndStatus(input.id(), EmployeeStatusEnum.ACTIVE).orElseThrow(
        () -> new NotFoundException("exception.profile.not-found")
    );

    if (input.name() != null && !employee.getName().equals(input.name())) employee.setName(input.name());
    if (input.phone() != null && !employee.getPhone().equals(input.phone())) employee.setPhone(input.phone());
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

    if (input.deviceToken() != null && !input.deviceToken().isBlank()) {
      deviceTokenRepository.save(new DeviceTokenDocument(employee.getId(), input.deviceToken()));
    }

    return ProfileMapper.convertEntityToUpdateProfileOutput(employee);
  }
}
