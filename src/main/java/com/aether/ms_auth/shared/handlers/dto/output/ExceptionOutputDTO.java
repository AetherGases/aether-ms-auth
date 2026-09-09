package com.aether.ms_auth.shared.handlers.dto.output;

import org.springframework.http.HttpStatus;

public record ExceptionOutputDTO(
    String message,
    HttpStatus status
) {
}
