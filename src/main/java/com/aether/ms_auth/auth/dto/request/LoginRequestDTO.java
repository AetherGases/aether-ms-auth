package com.aether.ms_auth.auth.dto.request;

import jakarta.validation.constraints.NotEmpty;

public record LoginRequestDTO(
    @NotEmpty(message = "{validation.email.required}")
    String email,

    @NotEmpty(message = "{validation.password.required}")
    String password
) {
}
