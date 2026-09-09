package com.aether.ms_auth.profile.dto.output;

import com.aether.ms_auth.shared.helpers.NormalizeOutput;
import com.aether.ms_auth.shared.persistence.postgres.entities.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class GetMyProfileInfosOutputDTO{

  @Getter
  @Setter
  @EqualsAndHashCode
  public static class Permission{
    private Integer id;
    private String name;
    private String description;

    public Permission(PermissionEntity entity) {
      this.id = entity.getId();
      this.name = NormalizeOutput.name(entity.getName());
      this.description = entity.getDescription();
    }
  }
  public static class Enterprise{
    private Integer id;
    private String name;
    private String cnpj;

    public Enterprise(EnterpriseEntity enterprise) {
      this.id = enterprise.getId();
      this.name = enterprise.getName();
      this.cnpj = enterprise.getCnpj();
    }
  }

  public static class Address{
    private Integer id;
    private String zipCode;
    private String state;
    private String city;
    private String neighborhood;
    private String street;
    private Integer number;
    private String complement;

    public Address(AddressEntity entity) {
      this.id = entity.getId();
      this.zipCode = entity.getZipCode();
      this.state = entity.getState();
      this.city = entity.getCity();
      this.neighborhood = entity.getNeighborhood();
      this.street = entity.getStreet();
      this.number = entity.getNumber();
      this.complement = entity.getComplement();
    }
  }

  public static class Unit{
    private Integer id;
    private String cnpj;

    public Unit(UnitEntity entity) {
      this.id = entity.getId();
      this.cnpj = cnpj;
    }
  }

  public static class Department{
    private Integer id;
    private String name;
    private String description;

    public Department(DepartmentEntity entity) {
      this.id = entity.getId();
      this.name = entity.getName();
      this.description = entity.getDescription();
    }
  }
}
