package ru.rgasymov.moneymanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Entity for storing report generation tasks with retry logic.
 */
@Entity
@Table(name = "report_tasks")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class ReportTask implements Serializable {
  @Serial
  private static final long serialVersionUID = 1234568L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  @ToString.Include
  private Long id;

  @Column(name = "telegram_id", nullable = false)
  private Long telegramId;

  @Column(name = "chat_id", nullable = false)
  private Long chatId;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private ReportTaskStatus status;

  @Column(name = "retry_count", nullable = false)
  private Integer retryCount = 0;

  @Column(name = "max_retries", nullable = false)
  private Integer maxRetries;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "next_retry_at")
  private LocalDateTime nextRetryAt;

  /**
   * Status of report generation task.
   */
  public enum ReportTaskStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
  }
}
