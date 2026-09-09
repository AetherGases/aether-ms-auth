package com.aether.ms_auth.auth.dto.request;

import com.aether.ms_auth.shared.AetherConstants;
import com.aether.ms_auth.shared.helpers.RegexPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordChangePasswordRequestDTO(
    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.regex}")
    String email,

    @NotBlank(message = "{validation.key.required}")
    @Size(
        min = AetherConstants.KEY_LENGTH,
        max = AetherConstants.KEY_LENGTH,
        message = "{validation.key.size}"
    )
    String key,

    @NotBlank(message = "{validation.password.required}")
    @Size(
        min = AetherConstants.MIN_PASSWORD_LENGTH,
        max = AetherConstants.MAX_PASSWORD_LENGTH,
        message = "{validation.password.size}"
    )
    @Pattern(
        regexp = RegexPatterns.PASSWORD,
        message = "{validation.password.regex}"
    )
    String password
) {
}
