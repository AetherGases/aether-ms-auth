package com.aether.ms_auth.profile.dto.output;

import java.time.LocalDate;
import java.util.List;

public record GetMyProfileOutputDTO(
    Integer id,
    String cpf,
    String email,
    String name,
    String phone,
    Image image,
    LocalDate createdAt,
    LocalDate updatedAt,
    List<Permission> permissions,
    Enterprise enterprise,
    Unit unit,
    Department department,
    Notifications notifications
) {
  public record Notifications(
      Boolean email,
      Boolean mobile
  ){}

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

  public record Enterprise(
      Integer id,
      String name,
      String tradeName,
      String cnpj,
      Address address
  ){}
  public record Unit(
      Integer id,
      String cnae,
      String cnpj,
      Boolean isActive,
      Address address
  ){}

  public record Department(
      Integer id,
      String name,
      String description
  ){}

  public record Image(
      Integer id,
      String name,
      String path
  ){}
}
