package com.aether.ms_auth.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

public record LoginRequestDTO(
    @NotEmpty(message = "{validation.email.required}")
    @Schema(
        description = "O e-mail do usuário a ser logado",
        example = "aether@dominio.com"
    )
    String email,

    @NotEmpty(message = "{validation.password.required}")
    @Schema(
        description = "A senha deste usuário",
        example = "Senha123"
    )
    String password
) {
}
