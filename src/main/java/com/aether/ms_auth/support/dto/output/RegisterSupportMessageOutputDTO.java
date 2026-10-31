package com.aether.ms_auth.support.dto.output;

import com.aether.ms_auth.shared.enums.ProblemCategoryEnum;

public record RegisterSupportMessageOutputDTO(
    Integer id,
    ProblemCategoryEnum category,
    String screen,
    String description,
    Image image
) {
  public record Image(
      Integer id,
      String name,
      String path
  ){}
}
