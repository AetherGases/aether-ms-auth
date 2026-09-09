package com.aether.ms_auth.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordSendCodeRequestDTO(
    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.regex}")
    String email
) {
}
