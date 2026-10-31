package com.aether.ms_auth.shared.persistence.postgres.entities;

import com.aether.ms_auth.shared.AetherConstants;
import com.aether.ms_auth.shared.enums.ProblemCategoryEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "reported_problem")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReportedProblemEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "PROBLEM_CATEGORY")
  private ProblemCategoryEnum category;

  @Column(length = AetherConstants.MAX_REPORT_PROBLEM_SCREEN_LENGTH)
  private String screen;
  @Column(length = AetherConstants.MAX_REPORT_PROBLEM_DESCRIPTION_LENGTH)
  private String description;

  @OneToOne
  @JoinColumn(name = "id_storage_file")
  private StorageFileEntity storageFile;

  public ReportedProblemEntity(ProblemCategoryEnum category, String screen, String description) {
    this.category = category;
    this.screen = screen;
    this.description = description;
  }

  public ReportedProblemEntity(ProblemCategoryEnum category, String screen, String description, StorageFileEntity storageFile) {
    this.category = category;
    this.screen = screen;
    this.description = description;
    this.storageFile = storageFile;
  }
}
