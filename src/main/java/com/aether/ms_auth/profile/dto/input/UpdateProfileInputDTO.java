package com.aether.ms_auth.profile.dto.input;

public record UpdateProfileInputDTO(
    Integer id,
    String name,
    String phone,
    String newPassword,
    Image image
) {

  public record Image(
      String imageName,
      String imageUrl
  ) {}
}