package com.aether.ms_auth.shared.exceptions;

import org.springframework.http.HttpStatus;

public abstract class MsAuthException extends RuntimeException{
  public MsAuthException(String message) {
    super(message);
  }

  public MsAuthException(String message, Throwable cause) {
    super(message, cause);
  }

  public abstract HttpStatus getStatus();
}