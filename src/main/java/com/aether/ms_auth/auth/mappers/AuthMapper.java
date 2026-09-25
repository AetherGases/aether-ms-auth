package com.aether.ms_auth.auth.mappers;

import com.aether.ms_auth.auth.dto.input.*;
import com.aether.ms_auth.auth.dto.output.RegisterOutputDTO;
import com.aether.ms_auth.auth.dto.request.*;
import com.aether.ms_auth.shared.helpers.NormalizeInput;
import com.aether.ms_auth.shared.persistence.postgres.entities.EmployeeEntity;

public class AuthMapper {
  public static LoginInputDTO convertLoginRequestToInput(LoginRequestDTO request){
    return new LoginInputDTO(
        NormalizeInput.email(request.email()),
        NormalizeInput.password(request.password())
    );
  }

  public static ResetPasswordSendCodeInputDTO convertSendCodeRequestToInput(ResetPasswordSendCodeRequestDTO request){
    return new ResetPasswordSendCodeInputDTO(
        NormalizeInput.email(request.email())
    );
  }

  public static ResetPasswordResendCodeInputDTO convertResendCodeRequestToInput(ResetPasswordResendCodeRequestDTO request){
    return new ResetPasswordResendCodeInputDTO(
        NormalizeInput.email(request.email())
    );
  }

  public static ResetPasswordValidateCodeInputDTO convertValidateCodeRequestToInput(ResetPasswordValidateCodeRequestDTO request){
    return new ResetPasswordValidateCodeInputDTO(
        NormalizeInput.email(request.email()),
        request.code()
    );
  }

  public static RegisterInputDTO convertRegisterRequestToInput(RegisterRequestDTO request){
    return new RegisterInputDTO(
        NormalizeInput.cpf(request.cpf()),
        NormalizeInput.name(request.name()),
        NormalizeInput.email(request.email()),
        NormalizeInput.password(request.password()),
        NormalizeInput.phone(request.phone())
    );
  }

  public static ResetPasswordChangePasswordInputDTO convertChangePasswordToInput(ResetPasswordChangePasswordRequestDTO request){
    return new ResetPasswordChangePasswordInputDTO(
        NormalizeInput.email(request.email()),
        request.key(),
        NormalizeInput.password(request.password())
    );
  }

  public static RegisterOutputDTO convertEntityToOutput(EmployeeEntity entity){
    return new RegisterOutputDTO(
        entity.getId(),
        entity.getCpf(),
        entity.getName(),
        entity.getEmail(),
        entity.getPhone()
    );
  }
}
