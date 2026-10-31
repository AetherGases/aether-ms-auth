package com.aether.ms_auth.profile.dto.input;

public record UpdateProfileInputDTO(
    Integer id,
    String name,
    String phone,
    String newPassword,
    String deviceToken,
    Notifications notifications,
    Image image
) {

  public record Notifications(
      Boolean email,
      Boolean mobile
  ) {}

  public record Image(
      String imageName,
      String imageUrl
  ) {}
}