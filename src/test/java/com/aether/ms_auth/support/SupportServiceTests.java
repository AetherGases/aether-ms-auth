package com.aether.ms_auth.support;

import com.aether.ms_auth.shared.enums.ProblemCategoryEnum;
import com.aether.ms_auth.shared.persistence.postgres.entities.ReportedProblemEntity;
import com.aether.ms_auth.shared.persistence.postgres.entities.StorageFileEntity;
import com.aether.ms_auth.shared.persistence.postgres.repositories.ReportedProblemRepository;
import com.aether.ms_auth.shared.persistence.postgres.repositories.StorageFileRepository;
import com.aether.ms_auth.support.dto.input.RegisterSupportMessageInputDTO;
import com.aether.ms_auth.support.dto.output.RegisterSupportMessageOutputDTO;
import com.aether.ms_auth.support.services.SupportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("SupportService Tests")
public class SupportServiceTests {
  @Mock
  private ReportedProblemRepository reportedProblemRepository;

  @Mock
  private StorageFileRepository storageFileRepository;

  @InjectMocks
  private SupportService supportService;

  private static final String SCREEN = "Histórico de Relatórios";
  private static final String DESCRIPTION = "Erro \"500 - Internal Server Error\" ao enviar um novo relatório.";

  private void stubSaveAssigningId(int id) {
    when(reportedProblemRepository.save(any(ReportedProblemEntity.class))).thenAnswer(invocation -> {
      ReportedProblemEntity entity = invocation.getArgument(0);
      entity.setId(id);
      return entity;
    });
  }

  @Test
  @DisplayName("Should persist the reported problem with the given category, screen and description")
  void registerSupportMessagePersistsEntity() {
    stubSaveAssigningId(1);

    supportService.registerSupportMessage(
        new RegisterSupportMessageInputDTO(ProblemCategoryEnum.BUG, SCREEN, DESCRIPTION, null)
    );

    ArgumentCaptor<ReportedProblemEntity> captor = ArgumentCaptor.forClass(ReportedProblemEntity.class);
    verify(reportedProblemRepository, times(1)).save(captor.capture());

    ReportedProblemEntity saved = captor.getValue();
    assertEquals(ProblemCategoryEnum.BUG, saved.getCategory());
    assertEquals(SCREEN, saved.getScreen());
    assertEquals(DESCRIPTION, saved.getDescription());
  }

  @Test
  @DisplayName("Should return the persisted data, including the generated id")
  void registerSupportMessageReturnsOutput() {
    stubSaveAssigningId(42);

    RegisterSupportMessageOutputDTO output = supportService.registerSupportMessage(
        new RegisterSupportMessageInputDTO(ProblemCategoryEnum.PERFORMANCE, SCREEN, DESCRIPTION, null)
    );

    assertEquals(42, output.id());
    assertEquals(ProblemCategoryEnum.PERFORMANCE, output.category());
    assertEquals(SCREEN, output.screen());
    assertEquals(DESCRIPTION, output.description());
  }

  @Test
  @DisplayName("Should return a null image since no attachment is sent")
  void registerSupportMessageWithoutImage() {
    stubSaveAssigningId(1);

    RegisterSupportMessageOutputDTO output = supportService.registerSupportMessage(
        new RegisterSupportMessageInputDTO(ProblemCategoryEnum.OTHERS, SCREEN, DESCRIPTION, null)
    );

    assertNull(output.image());
  }

  @Test
  @DisplayName("Should not create a storage file when no image is sent")
  void registerSupportMessageWithoutImageDoesNotTouchStorage() {
    stubSaveAssigningId(1);

    supportService.registerSupportMessage(
        new RegisterSupportMessageInputDTO(ProblemCategoryEnum.OTHERS, SCREEN, DESCRIPTION, null)
    );

    verify(storageFileRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should persist the image and link it to the reported problem")
  void registerSupportMessageWithImage() {
    when(storageFileRepository.save(any(StorageFileEntity.class))).thenAnswer(invocation -> {
      StorageFileEntity file = invocation.getArgument(0);
      file.setId(7);
      return file;
    });
    stubSaveAssigningId(1);

    RegisterSupportMessageOutputDTO output = supportService.registerSupportMessage(
        new RegisterSupportMessageInputDTO(
            ProblemCategoryEnum.BUG, SCREEN, DESCRIPTION,
            new RegisterSupportMessageInputDTO.Image("print.png", "https://cdn/print.png")
        )
    );

    ArgumentCaptor<ReportedProblemEntity> captor = ArgumentCaptor.forClass(ReportedProblemEntity.class);
    verify(reportedProblemRepository).save(captor.capture());
    assertEquals("print.png", captor.getValue().getStorageFile().getName());

    assertEquals(7, output.image().id());
    assertEquals("print.png", output.image().name());
    assertEquals("https://cdn/print.png", output.image().path());
  }

  @Test
  @DisplayName("Should not swallow repository errors")
  void registerSupportMessageRepositoryFailure() {
    when(reportedProblemRepository.save(any(ReportedProblemEntity.class)))
        .thenThrow(new IllegalStateException("db down"));

    assertThrows(
        IllegalStateException.class,
        () -> supportService.registerSupportMessage(
            new RegisterSupportMessageInputDTO(ProblemCategoryEnum.ERROR, SCREEN, DESCRIPTION, null)
        )
    );
  }
}
