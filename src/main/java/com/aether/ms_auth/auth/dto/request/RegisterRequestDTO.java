package com.aether.ms_auth.auth.dto.request;

import com.aether.ms_auth.shared.AetherConstants;
import com.aether.ms_auth.shared.helpers.RegexPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
    @NotEmpty(message = "{validation.cpf.required}")
    @Pattern(
        regexp = RegexPatterns.CPF,
        message = "{validation.cpf.regex}"
    )
    String cpf,

    @NotEmpty(message = "{validation.name.required}")
    @Size(
        min = AetherConstants.MIN_EMPLOYEE_NAME_LENGTH,
        max = AetherConstants.MAX_EMPLOYEE_NAME_LENGTH,
        message = "{validation.name.size}"
    )
    @Pattern(
        regexp = RegexPatterns.NAME,
        message = "{validation.name.regex}"
    )
    String name,

    @NotEmpty(message = "{validation.email.required}")
    @Email(message = "{validation.email.regex}")
    String email,

    @NotEmpty(message = "{validation.password.required}")
    @Size(
        min = AetherConstants.MIN_PASSWORD_LENGTH,
        max = AetherConstants.MAX_PASSWORD_LENGTH,
        message = "{validation.password.size}"
    )
    @Pattern(
        regexp = RegexPatterns.PASSWORD,
        message = "{validation.password.regex}"
    )
    String password,

    @NotEmpty(message = "{validation.phone.required}")
    @Pattern(
        regexp = RegexPatterns.PHONE,
        message = "{validation.phone.regex}"
    )
    String phone

) {
}