package com.aether.ms_auth.notification.controllers;

import com.aether.ms_auth.notification.dto.input.FindManyNotificationsInputDTO;
import com.aether.ms_auth.notification.dto.output.FindManyNotificationsOutputDTO;
import com.aether.ms_auth.notification.dto.query.FindManyNotificationsQueryParamsDTO;
import com.aether.ms_auth.notification.services.NotificationService;
import com.aether.ms_auth.shared.docs.NotificationControllerDocs;
import com.aether.ms_auth.shared.helpers.CustomUserDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "Rotas para visualização das notificações do usuário logado.")
@RestController
@RequestMapping("/api/profile/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerDocs {
  private final NotificationService notificationService;

  @Override
  @GetMapping()
  public ResponseEntity<FindManyNotificationsOutputDTO> findManyNotifications(
      Authentication authentication,

      @ModelAttribute
      @Valid
      FindManyNotificationsQueryParamsDTO query
  ){
    CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

    return new ResponseEntity<>(
        this.notificationService.findManyNotifications(
            new FindManyNotificationsInputDTO(user.getId(), query.skip(), query.take())
        ),
        HttpStatus.OK
    );
  }
}
