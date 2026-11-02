package com.aether.ms_auth.support.services;

import com.aether.ms_auth.shared.enums.ProblemCategoryEnum;
import com.aether.ms_auth.shared.persistence.postgres.entities.ReportedProblemEntity;
import com.aether.ms_auth.shared.persistence.postgres.entities.StorageFileEntity;
import com.aether.ms_auth.shared.persistence.postgres.repositories.ReportedProblemRepository;
import com.aether.ms_auth.shared.persistence.postgres.repositories.StorageFileRepository;
import com.aether.ms_auth.support.dto.input.RegisterSupportMessageInputDTO;
import com.aether.ms_auth.support.dto.output.FindProblemCategoriesOutputDTO;
import com.aether.ms_auth.support.dto.output.RegisterSupportMessageOutputDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportService {
  private final ReportedProblemRepository reportedProblemRepository;
  private final StorageFileRepository storageFileRepository;

  public List<FindProblemCategoriesOutputDTO> findProblemCategories() {
    return Arrays.stream(ProblemCategoryEnum.values())
        .map(category -> new FindProblemCategoriesOutputDTO(category.name(), category.getLabel()))
        .toList();
  }

  @Transactional
  public RegisterSupportMessageOutputDTO registerSupportMessage(RegisterSupportMessageInputDTO input) {
    StorageFileEntity storageFile = null;

    if (input.image() != null) {
      storageFile = this.storageFileRepository.save(
          new StorageFileEntity(input.image().name(), input.image().path())
      );
    }

    ReportedProblemEntity reportedProblem = this.reportedProblemRepository.save(
        new ReportedProblemEntity(
            input.category(),
            input.screen(),
            input.description(),
            storageFile
        )
    );

    return new RegisterSupportMessageOutputDTO(
        reportedProblem.getId(),
        reportedProblem.getCategory(),
        reportedProblem.getScreen(),
        reportedProblem.getDescription(),
        storageFile != null
            ? new RegisterSupportMessageOutputDTO.Image(
                storageFile.getId(),
                storageFile.getName(),
                storageFile.getPath()
            )
            : null
    );
  }
}
