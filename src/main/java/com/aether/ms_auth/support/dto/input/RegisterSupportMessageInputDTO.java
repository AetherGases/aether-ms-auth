package com.aether.ms_auth.support.dto.input;

import com.aether.ms_auth.shared.enums.ProblemCategoryEnum;

public record RegisterSupportMessageInputDTO(
    ProblemCategoryEnum category,

    String screen,
    String description,
    Image image
) {
  public record Image(
    String name,
    String path
  ){}
}
