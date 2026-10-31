package com.aether.ms_auth.support.dto.request;

import com.aether.ms_auth.shared.AetherConstants;
import com.aether.ms_auth.shared.enums.ProblemCategoryEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterSupportMessageRequestDTO(
    @NotNull(message = "{validation.category.required}")
    ProblemCategoryEnum category,

    @NotBlank(message = "{validation.screen.required}")
    @Size(
        min = AetherConstants.MIN_REPORT_PROBLEM_SCREEN_LENGTH,
        max = AetherConstants.MAX_REPORT_PROBLEM_SCREEN_LENGTH,
        message = "{validation.screen.size}"
    )
    String screen,

    @NotBlank(message = "{validation.description.required}")
    @Size(
        min = AetherConstants.MIN_REPORT_PROBLEM_DESCRIPTION_LENGTH,
        max = AetherConstants.MAX_REPORT_PROBLEM_DESCRIPTION_LENGTH,
        message = "{validation.description.size}"
    )
    String description,

    @Valid
    ImageRequest image
) {
  public record ImageRequest(
      @NotEmpty(message = "{validation.image-name.required}")
      @Size(
          min = AetherConstants.MIN_STORAGE_FILE_NAME_LENGTH,
          max = AetherConstants.MAX_STORAGE_FILE_NAME_LENGTH,
          message = "{validation.image-name.size}"
      )
      String imageName,

      @NotEmpty(message = "{validation.image-url.required}")
      @Size(
          min = AetherConstants.MIN_STORAGE_FILE_PATH_LENGTH,
          max = AetherConstants.MAX_STORAGE_FILE_PATH_LENGTH,
          message = "{validation.image-url.size}"
      )
      @URL(protocol = "https", message = "{validation.image-url.regex}")
      String imageUrl
  ) {}
}
