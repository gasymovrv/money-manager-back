package ru.rgasymov.moneymanager.service.report;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.rgasymov.moneymanager.domain.entity.ReportTask;
import ru.rgasymov.moneymanager.domain.entity.ReportTask.ReportTaskStatus;
import ru.rgasymov.moneymanager.repository.ReportTaskRepository;
import ru.rgasymov.moneymanager.service.telegram.TelegramBotClient;

/**
 * Service for processing report generation tasks with retry logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportTaskProcessor {

  private final TransactionTemplate transactionTemplate;
  private final ReportTaskRepository reportTaskRepository;
  private final ReportGenerationService reportGenerationService;
  private final TelegramBotClient telegramBotClient;

  @Value("${report.task.retry-delay-minutes:5}")
  private int retryDelayMinutes;

  @Value("${report.task.cleanup.retention-days:30}")
  private int cleanupRetentionDays;

  /**
   * Process pending report tasks.
   * Runs every minute by default.
   */
  @Scheduled(fixedDelayString = "${report.task.processor.delay-ms:60000}")
  // TODO Все еще транзакция? внутри по прежнему генерация репорта. Сделать лямбда утилиту чтобы обернуть транзакциями методы updateTaskStatus и handleFailure
  @Transactional
  public void processPendingTasks() {
    // Use SELECT FOR UPDATE SKIP LOCKED to prevent concurrent processing across multiple pods
    List<ReportTask> pendingTasks = reportTaskRepository.findTasksForProcessing(ReportTaskStatus.PENDING.name(), LocalDateTime.now());

    if (pendingTasks.isEmpty()) {
      log.debug("No pending tasks to process");
      return;
    }

    log.info("Processing {} pending report tasks", pendingTasks.size());

    for (ReportTask task : pendingTasks) {
      processTask(task);
    }
  }

  /**
   * Process a single report task.
   *
   * @param task the task to process
   */
  private void processTask(ReportTask task) {
    log.info("Processing report task {} for user {}", task.getId(), task.getTelegramId());

    updateTaskStatus(task, ReportTaskStatus.PROCESSING);

    File reportFile = null;
    try {
      // Generate report outside of transaction (heavy operation)
      reportFile = reportGenerationService.generateReport(task.getTelegramId(), task.getStartDate(), task.getEndDate());

      // Send report to user (throws exception on failure)
      telegramBotClient.sendDocument(
          task.getChatId(),
          reportFile,
          String.format("Financial report for period %s - %s", task.getStartDate(), task.getEndDate())
      );

      updateTaskStatus(task, ReportTaskStatus.COMPLETED);
      log.info("Report task {} completed successfully", task.getId());

    } catch (Exception e) {
      log.error("Error processing report task {}", task.getId(), e);
      handleFailure(task, e.getMessage());
    } finally {
      // Delete temporary file
      if (reportFile != null && reportFile.exists()) {
        reportFile.delete();
      }
    }
  }

  /**
   * Update task status in a separate transaction.
   */
  private void updateTaskStatus(ReportTask task, ReportTaskStatus status) {
    task.setStatus(status);
    task.setUpdatedAt(LocalDateTime.now());
    reportTaskRepository.save(task);
  }

  /**
   * Handle task failure with retry logic.
   *
   * @param task         the failed task
   * @param errorMessage the error message
   */
  private void handleFailure(ReportTask task, String errorMessage) {
    task.setRetryCount(task.getRetryCount() + 1);
    task.setErrorMessage(errorMessage);
    task.setUpdatedAt(LocalDateTime.now());

    if (task.getRetryCount() >= task.getMaxRetries()) {
      // Max retries reached, mark as failed
      task.setStatus(ReportTaskStatus.FAILED);
      reportTaskRepository.save(task);

      log.error("Report task {} failed after {} retries", task.getId(), task.getRetryCount());

      // Notify user about failure
      telegramBotClient.sendMessage(
          task.getChatId(),
          "Sorry, we were unable to generate your report. Please try again later or contact support if the problem persists."
      );
    } else {
      // Schedule retry
      task.setStatus(ReportTaskStatus.PENDING);
      task.setNextRetryAt(LocalDateTime.now().plusMinutes(retryDelayMinutes));
      reportTaskRepository.save(task);

      log.info("Report task {} scheduled for retry {} at {}", task.getId(), task.getRetryCount(), task.getNextRetryAt());
    }
  }

  /**
   * Cleanup old completed and failed tasks.
   * Runs daily at 3 AM by default.
   */
  @Scheduled(cron = "${report.task.cleanup.cron:0 0 3 * * ?}")
  @Transactional
  public void cleanupOldTasks() {
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cleanupRetentionDays);

    int deletedCount = reportTaskRepository.deleteOldTasks(
        List.of(ReportTaskStatus.COMPLETED, ReportTaskStatus.FAILED, ReportTaskStatus.PROCESSING),
        cutoffDate
    );

    if (deletedCount > 0) {
      log.info("Cleaned up {} old report tasks older than {}", deletedCount, cutoffDate);
    }
  }
}
