package com.aether.ms_auth.auth.dto.input;

public record ResetPasswordValidateCodeInputDTO(
    String email,
    String code
) {
}
