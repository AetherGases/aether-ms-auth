package com.aether.ms_auth.profile.mappers;

import com.aether.ms_auth.profile.dto.output.GetMyProfileInfosOutputDTO;
import com.aether.ms_auth.profile.dto.output.GetMyProfileOutputDTO;
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
}
