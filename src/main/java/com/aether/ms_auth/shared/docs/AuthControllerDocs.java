package com.aether.ms_auth.shared.docs;

import com.aether.ms_auth.auth.dto.output.LoginOutputDTO;
import com.aether.ms_auth.auth.dto.output.RegisterOutputDTO;
import com.aether.ms_auth.auth.dto.request.LoginRequestDTO;
import com.aether.ms_auth.auth.dto.request.RegisterRequestDTO;
import com.aether.ms_auth.auth.dto.request.ResetPasswordSendCodeRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public interface AuthControllerDocs {
  @Operation(
      summary = "Realiza login um usuário no sistema.",
      description = "Busca e valida a senha de um usuário.",
      tags = {"Auth"},
      responses = {
          @ApiResponse(description = "Success", responseCode = "200", content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = LoginOutputDTO.class))
          )
          ),
          @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content),
          @ApiResponse(description = "Unhautorized", responseCode = "401", content = @Content),
          @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
          @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content)
      }
  )
  ResponseEntity<LoginOutputDTO> login(
      @Valid LoginRequestDTO input
  );

  @Operation(
      summary = "Realiza o refresh de um token JWT de um usuário já autenticado.",
      description = "Realiza o refresh de um token JWT.",
      tags = {"Auth"},
      responses = {
          @ApiResponse(description = "Success", responseCode = "200", content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = LoginOutputDTO.class))
          )
          ),
          @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content),
          @ApiResponse(description = "Unhautorized", responseCode = "401", content = @Content),
          @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
          @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content)
      }
  )
  ResponseEntity<LoginOutputDTO> refreshToken(
      @Valid
      @Email(message = "{validation.email.regex}")
      String email,

      @NotEmpty(message = "{validation.refresh-token.required}")
      String refreshToken
  );

  @Operation(
      summary = "Envia um código para o e-mail informado.",
      description = "Envia um código para o e-mail informado se o mesmo estiver ativo.",
      tags = {"Auth"},
      responses = {
          @ApiResponse(description = "No content", responseCode = "204", content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = Void.class))
          )
          ),
          @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content),
          @ApiResponse(description = "Unhautorized", responseCode = "401", content = @Content),
          @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
          @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content)
      }
  )
  ResponseEntity<Void> resetPasswordSendCode(
      @Valid
      ResetPasswordSendCodeRequestDTO input
  );

  // TO DO: remove this route after SQL's dataload is ready to execute, registering users is a first year responsability
  @Operation(
      summary = "Cadastra um funcionário no sistema, rota que deve ser removida após o dataload ficar pronto.",
      description = "Cadastra um usuário no sistema.",
      tags = {"Auth"},
      responses = {
          @ApiResponse(description = "Success", responseCode = "200", content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = RegisterOutputDTO.class))
          )
          ),
          @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content),
          @ApiResponse(description = "Unhautorized", responseCode = "401", content = @Content),
          @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
          @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content)
      }
  )
  ResponseEntity<RegisterOutputDTO> register(
      @Valid
      RegisterRequestDTO input
  );
}
