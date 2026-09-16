package com.aether.ms_auth.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordSendCodeRequestDTO(
    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.regex}")
    @Schema(
        description = "O e-mail do usuário",
        example = "aether@dominio.com"
    )
    String email
) {
}
