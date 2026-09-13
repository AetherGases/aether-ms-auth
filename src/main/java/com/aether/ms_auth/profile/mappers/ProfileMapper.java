package com.aether.ms_auth.profile.mappers;

import com.aether.ms_auth.profile.dto.input.UpdateProfileInputDTO;
import com.aether.ms_auth.profile.dto.output.GetMyProfileInfosOutputDTO;
import com.aether.ms_auth.profile.dto.output.GetMyProfileOutputDTO;
import com.aether.ms_auth.profile.dto.output.UpdateProfileOutputDTO;
import com.aether.ms_auth.profile.dto.request.UpdateProfileRequestDTO;
import com.aether.ms_auth.shared.helpers.NormalizeInput;
import com.aether.ms_auth.shared.helpers.NormalizeOutput;
import com.aether.ms_auth.shared.persistence.postgres.entities.EmployeeEntity;

import java.util.List;

public class ProfileMapper {
  public static GetMyProfileOutputDTO convertEntityToGetProfileOutput(EmployeeEntity entity){
    List<GetMyProfileInfosOutputDTO.Permission> permissions = entity.getPermissionGroups()
        .stream()
        .flatMap(pg -> pg.getPermissions().stream())
        .distinct()
        .map(GetMyProfileInfosOutputDTO.Permission::new)
        .toList();

    return new GetMyProfileOutputDTO(
        entity.getId(),
        NormalizeOutput.cpf(entity.getCpf()),
        entity.getEmail(),
        NormalizeOutput.name(entity.getName()),
        permissions
    );
  }

  public static UpdateProfileInputDTO convertRequestToUpdateProfileInput(UpdateProfileRequestDTO request, Integer userId){
    return new UpdateProfileInputDTO(
        userId,
        NormalizeInput.name(request.name()),
        NormalizeInput.phone(request.phone()),
        NormalizeInput.password(request.newPassword()),
        new UpdateProfileInputDTO.Image(
            request.image().imageName(),
            request.image().imageUrl()
        )
    );
  }

  public static UpdateProfileOutputDTO convertEntityToUpdateProfileOutput(EmployeeEntity entity){
    return new UpdateProfileOutputDTO(
        entity.getId(),
        entity.getName(),
        entity.getCpf(),
        entity.getEmail(),
        entity.getPhone()
    );
  }
}
