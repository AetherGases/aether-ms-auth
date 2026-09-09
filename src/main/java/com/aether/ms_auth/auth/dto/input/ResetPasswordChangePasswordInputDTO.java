package com.aether.ms_auth.auth.dto.input;

public record ResetPasswordChangePasswordInputDTO(
    String email,
    String key,
    String password
) {
}
