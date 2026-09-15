package com.aether.ms_auth.profile.services;

import com.aether.ms_auth.profile.dto.input.GetMyProfileInputDTO;
import com.aether.ms_auth.profile.dto.output.GetMyProfileOutputDTO;
import com.aether.ms_auth.profile.mappers.ProfileMapper;
import com.aether.ms_auth.shared.enums.EmployeeStatusEnum;
import com.aether.ms_auth.shared.exceptions.NotFoundException;
import com.aether.ms_auth.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_auth.shared.persistence.postgres.repositories.EmployeeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {
  private final EmployeeRepository employeeRepository;

  @Transactional
  public GetMyProfileOutputDTO getMyProfile(GetMyProfileInputDTO input){
    EmployeeEntity employee = employeeRepository.findByIdAndStatus(input.id(), EmployeeStatusEnum.ACTIVE).orElseThrow(
        () -> new NotFoundException("exception.profile.not-found")
    );

    return ProfileMapper.convertEntityToGetProfileOutput(employee);
  }
}
