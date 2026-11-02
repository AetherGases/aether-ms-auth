package com.aether.ms_auth.support.dto.output;

import io.swagger.v3.oas.annotations.media.Schema;

public record FindProblemCategoriesOutputDTO(
    @Schema(description = "Valor do enum, enviado de volta ao registrar o problema", example = "LOGIN_AND_ACCOUNT")
    String code,
    @Schema(description = "Texto exibido ao usuário", example = "Login e conta")
    String label
) {}
