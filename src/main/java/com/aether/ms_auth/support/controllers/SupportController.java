package com.aether.ms_auth.support.controllers;

import com.aether.ms_auth.support.dto.output.RegisterSupportMessageOutputDTO;
import com.aether.ms_auth.support.dto.request.RegisterSupportMessageRequestDTO;
import com.aether.ms_auth.support.mappers.SupportMapper;
import com.aether.ms_auth.support.services.SupportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Support", description = "Rotas para suporte e reporte de problemas.")
@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {
  private final SupportService supportService;

  @PostMapping
  public ResponseEntity<RegisterSupportMessageOutputDTO> registerSupportMessage(
      @RequestBody
      @Valid
      RegisterSupportMessageRequestDTO request
  ) {
    return new ResponseEntity<>(
        this.supportService.registerSupportMessage(
            SupportMapper.convertRequestToRegisterSupportMessageInput(request)
        ),
        HttpStatus.OK
    );
  }
}
