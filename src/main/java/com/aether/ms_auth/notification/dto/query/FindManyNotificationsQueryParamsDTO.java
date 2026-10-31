package com.aether.ms_auth.notification.dto.query;

import com.aether.ms_auth.shared.AetherConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record FindManyNotificationsQueryParamsDTO(
    @Min(value = AetherConstants.MIN_SKIP, message = "{validation.skip.min}")
    @Schema(description = "Quantidade de itens a pular (padrão 0).", example = "0", defaultValue = "0")
    Integer skip,

    @Min(value = AetherConstants.MIN_TAKE, message = "{validation.take.min}")
    @Max(value = AetherConstants.MAX_TAKE, message = "{validation.take.max}")
    @Schema(description = "Quantidade máxima de itens retornados (padrão 20, máximo 100).", example = "20", defaultValue = "20")
    Integer take
) {
  public FindManyNotificationsQueryParamsDTO {
    skip = skip == null ? AetherConstants.DEFAULT_SKIP : skip;
    take = take == null ? AetherConstants.DEFAULT_TAKE : take;
  }
}
