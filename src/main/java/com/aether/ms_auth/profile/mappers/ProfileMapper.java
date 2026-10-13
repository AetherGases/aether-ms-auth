package com.aether.ms_auth.profile.mappers;

import com.aether.ms_auth.profile.dto.input.UpdateProfileInputDTO;
import com.aether.ms_auth.profile.dto.output.GetMyProfileOutputDTO;
import com.aether.ms_auth.profile.dto.output.UpdateProfileOutputDTO;
import com.aether.ms_auth.profile.dto.request.UpdateProfileRequestDTO;
import com.aether.ms_auth.shared.helpers.NormalizeInput;
import com.aether.ms_auth.shared.helpers.NormalizeOutput;
import com.aether.ms_auth.shared.persistence.postgres.entities.*;

public class ProfileMapper {
  public static GetMyProfileOutputDTO convertEntityToGetProfileOutput(
      EmployeeEntity entity
  ) {
    DepartmentEntity department = entity.getDepartment();
    UnitEntity unit = department.getUnit();
    EnterpriseEntity enterprise = unit.getEnterprise();
    AddressEntity unitAddress = unit.getAddress();
    AddressEntity enterpriseAddress = enterprise.getAddress();
    StorageFileEntity image = entity.getStorageFile();

    return new GetMyProfileOutputDTO(
        entity.getId(),
        NormalizeOutput.cpf(entity.getCpf()),
        NormalizeOutput.email(entity.getEmail()),
        NormalizeOutput.name(entity.getName()),
        NormalizeOutput.phone(entity.getPhone()),

        image != null ? new GetMyProfileOutputDTO.Image(
            image.getId(),
            image.getName(),
            image.getPath()
        ) : null,

        entity.getPermissionGroup()
            .getPermissions()
            .stream()
            .map(p -> new GetMyProfileOutputDTO.Permission(
                p.getId(),
                p.getName(),
                p.getDescription()
            ))
            .toList(),

        new GetMyProfileOutputDTO.Enterprise(
            enterprise.getId(),
            enterprise.getName(),
            enterprise.getTradeName(),
            enterprise.getCnpj(),
            enterpriseAddress != null ? new GetMyProfileOutputDTO.Address(
                enterpriseAddress.getId(),
                enterpriseAddress.getZipCode(),
                enterpriseAddress.getState(),
                enterpriseAddress.getCity(),
                enterpriseAddress.getNeighborhood(),
                enterpriseAddress.getStreet(),
                enterpriseAddress.getNumber(),
                enterpriseAddress.getComplement()
            ) : null
        ),

        new GetMyProfileOutputDTO.Unit(
            unit.getId(),
            unit.getCnae(),
            unit.getCnpj(),
            unit.getIsActive(),

            unitAddress != null ? new GetMyProfileOutputDTO.Address(
                unitAddress.getId(),
                unitAddress.getZipCode(),
                unitAddress.getState(),
                unitAddress.getCity(),
                unitAddress.getNeighborhood(),
                unitAddress.getStreet(),
                unitAddress.getNumber(),
                unitAddress.getComplement()
            ): null
        )
    );
  }

  public static UpdateProfileInputDTO convertRequestToUpdateProfileInput(UpdateProfileRequestDTO request, Integer userId){
    return new UpdateProfileInputDTO(
        userId,
        NormalizeInput.name(request.name()),
        NormalizeInput.phone(request.phone()),
        NormalizeInput.password(request.newPassword()),
        request.image() != null
            ? new UpdateProfileInputDTO.Image(
            request.image().imageName(),
            request.image().imageUrl()
        ) : null
    );
  }

  public static UpdateProfileOutputDTO convertEntityToUpdateProfileOutput(EmployeeEntity entity){
    return new UpdateProfileOutputDTO(
        entity.getId(),
        NormalizeOutput.name(entity.getName()),
        NormalizeOutput.cpf(entity.getCpf()),
        NormalizeOutput.email(entity.getEmail()),
        NormalizeOutput.phone(entity.getPhone())
    );
  }
}
