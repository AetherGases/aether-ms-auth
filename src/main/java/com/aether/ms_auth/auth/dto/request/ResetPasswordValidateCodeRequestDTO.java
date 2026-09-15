package com.aether.ms_auth.auth.dto.request;

import com.aether.ms_auth.shared.helpers.RegexPatterns;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordValidateCodeRequestDTO(
    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.regex}")
    @Schema(
        description = "O e-mail do usuário",
        example = "aether@dominio.com"
    )
    String email,

    @NotBlank(message = "{validation.code.required}")
    @Pattern(
        regexp = RegexPatterns.CODE,
        message = "{validation.code.regex}"
    )
    @Schema(
        description = "O código enviado via e-mail",
        example = "123456"
    )
    String code
) {
}
