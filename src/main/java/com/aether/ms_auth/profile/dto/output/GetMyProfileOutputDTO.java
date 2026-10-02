package com.aether.ms_auth.profile.dto.output;

import java.util.List;

public record GetMyProfileOutputDTO(
    Integer id,
    String cpf,
    String email,
    String name,
    String phone,
    Image image,
    List<Permission> permissions,
    Unit unit
) {
  public record Permission(
      Integer id,
      String name,
      String description
  ){
  }

  public record Address(
      Integer id,
      String zipCode,
      String state,
      String city,
      String neighborhood,
      String street,
      Integer number,
      String complement
  ){}

  public record Unit(
      Integer id,
      String cnae,
      String cnpj,
      Boolean isActive,
      Address address
  ){}

  public record Image(
      Integer id,
      String name,
      String path
  ){}
}
