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

  private final ReportTaskRepository reportTaskRepository;
  private final ReportGenerationService reportGenerationService;
  private final TelegramBotClient telegramBotClient;

  @Value("${report.task.retry-delay-minutes:5}")
  private int retryDelayMinutes;

  /**
   * Process pending report tasks.
   * Runs every minute by default.
   */
  @Scheduled(fixedDelayString = "${report.task.processor.delay-ms:60000}")
  @Transactional
  public void processPendingTasks() {
    // TODO Что если второй под сервиса также вызовет эту ручку? нужно делать SELECT ... FOR UPDATE SKIP LOCKED и предполагаю тут  лучше через native query
    List<ReportTask> pendingTasks = reportTaskRepository.findByStatusAndNextRetryAtLessThanEqual(ReportTaskStatus.PENDING, LocalDateTime.now());

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

    task.setStatus(ReportTaskStatus.PROCESSING);
    task.setUpdatedAt(LocalDateTime.now());
    reportTaskRepository.save(task);

    try {
      // TODO Такая тяжелая операция внутри транзакции? может стоит разделить на 2 транзакции
      // Generate report
      File reportFile = reportGenerationService.generateReport(task.getTelegramId(), task.getStartDate(), task.getEndDate());

      // TODO Зачем нам этот boolean? Лучше кинуть ошибку и обработать все в одном catch блоке
      // Send report to user
      boolean sent = telegramBotClient.sendDocument(
          task.getChatId(),
          reportFile,
          String.format("Financial report for period %s - %s", task.getStartDate(), task.getEndDate())
      );

      if (sent) {
        task.setStatus(ReportTaskStatus.COMPLETED);
        task.setUpdatedAt(LocalDateTime.now());
        // TODO решил не удалять таски в случае успеха или ошибки после всех ретраев? тогда нужен другой шедулер который будет чистить старые по истечении какого-то времени
        reportTaskRepository.save(task);
        log.info("Report task {} completed successfully", task.getId());

        // Delete temporary file
        if (reportFile.exists()) {
          reportFile.delete();
        }
      } else {
        handleFailure(task, "Failed to send report to Telegram");
      }

    } catch (Exception e) {
      log.error("Error processing report task {}", task.getId(), e);
      handleFailure(task, e.getMessage());
    }
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
}
