package ru.rgasymov.moneymanager.service.report;

import jakarta.annotation.PreDestroy;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
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

  @Value("${report.task.batch-size:10}")
  private int batchSize;

  @Value("${report.task.max-parallel-tasks:5}")
  private int maxParallelTasks;

  // Virtual thread executor for I/O-bound report generation
  private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

  // Semaphore to limit concurrent tasks
  private final Semaphore taskSemaphore = new Semaphore(maxParallelTasks);

  /**
   * Process pending report tasks.
   * Runs every minute by default.
   * Uses micro-transactions and virtual threads for parallel processing.
   */
  @Scheduled(fixedDelayString = "${report.task.processor.delay-ms:60000}")
  public void processPendingTasks() {
    try {
      List<ReportTask> processingTasks = transactionTemplate.execute(_ -> {
        List<ReportTask> pendingTasks = reportTaskRepository.findTasksForProcessing(ReportTaskStatus.PENDING.name(), batchSize);
        for (ReportTask task : pendingTasks) {
          task.setStatus(ReportTaskStatus.PROCESSING);
          task.setUpdatedAt(LocalDateTime.now());
          reportTaskRepository.save(task);
        }
        return pendingTasks;
      });

      if (processingTasks == null || processingTasks.isEmpty()) {
        log.debug("No tasks to process");
        return;
      }

      log.info("Found {} pending report tasks, submitting for parallel processing", processingTasks.size());

      // TODO We need to process parallelly only for different users
      // Submit each task to virtual thread executor
      // Virtual threads are perfect for I/O-bound operations like report generation
      for (ReportTask task : processingTasks) {
        virtualThreadExecutor.submit(() -> processTaskAsync(task));
      }

    } catch (Exception e) {
      log.error("Error in processPendingTasks scheduler", e);
    }
  }

  @PreDestroy
  public void stop() {
    virtualThreadExecutor.shutdown();
    try {
      if (!virtualThreadExecutor.awaitTermination(3, TimeUnit.MINUTES)) {
        log.warn("Workers did not terminate in time, forcing shutdownNow()");
        virtualThreadExecutor.shutdownNow();
        virtualThreadExecutor.awaitTermination(5, TimeUnit.SECONDS);
      }
    } catch (InterruptedException ie) {
      virtualThreadExecutor.shutdownNow();
      log.info("Executor stopped forcibly");
      Thread.currentThread().interrupt();
    }
    log.info("Executor stopped successfully");
  }

  /**
   * Process a single report task asynchronously.
   * Acquires semaphore to limit concurrent processing.
   *
   * @param task the task to process
   */
  private void processTaskAsync(ReportTask task) {
    try {
      taskSemaphore.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Interrupted while waiting for semaphore for task {}", task.getId());
      return;
    }

    try {
      processTask(task);
    } finally {
      // Always release semaphore
      taskSemaphore.release();
    }
  }

  /**
   * Process task with separate micro-transactions for each step.
   * This prevents long-running transactions.
   */
  private void processTask(ReportTask task) {
    log.info("Processing report task {} for user {} (thread: {})", task.getId(), task.getTelegramId(), Thread.currentThread());

    File reportFile = null;
    try {
      // Generate report (NO transaction - heavy I/O operation)
      // If OOM happens here, transaction is already committed, task stays in PROCESSING
      // Cleanup scheduler will eventually mark it as failed
      reportFile = reportGenerationService.generateReport(task.getTelegramId(), task.getStartDate(), task.getEndDate());

      // Send report (NO transaction - I/O operation)
      telegramBotClient.sendDocument(
          task.getChatId(),
          reportFile,
          String.format("Financial report for period %s - %s", task.getStartDate(), task.getEndDate())
      );

      // Mark as completed (micro-transaction)
      transactionTemplate.executeWithoutResult(_ -> {
        task.setStatus(ReportTaskStatus.COMPLETED);
        task.setUpdatedAt(LocalDateTime.now());
        reportTaskRepository.save(task);
      });

      log.info("Report task {} completed successfully", task.getId());

    } catch (Exception e) {
      log.error("Error processing report task {}", task.getId(), e);
      // Handle failure in separate transaction
      try {
        handleFailure(task, e.getMessage());
      } catch (Exception failureEx) {
        log.error("Failed to handle failure for task {}", task.getId(), failureEx);
      }

    } finally {
      // Always clean up temp file
      if (reportFile != null && reportFile.exists()) {
        try {
          reportFile.delete();
        } catch (Exception e) {
          log.warn("Failed to delete temp file: {}", reportFile.getAbsolutePath(), e);
        }
      }
    }
  }

  /**
   * Handle task failure with retry logic in a separate transaction.
   *
   * @param task         the task
   * @param errorMessage the error message
   */
  private void handleFailure(ReportTask task, String errorMessage) {
    transactionTemplate.executeWithoutResult(_ -> {
      task.setRetryCount(task.getRetryCount() + 1);
      task.setErrorMessage(errorMessage);
      task.setUpdatedAt(LocalDateTime.now());

      if (task.getRetryCount() >= task.getMaxRetries()) {
        // Max retries reached, mark as failed
        task.setStatus(ReportTaskStatus.FAILED);
        reportTaskRepository.save(task);

        log.error("Report task {} failed after {} retries", task.getId(), task.getRetryCount());

        notifyUserAboutFailure(task);
      } else {
        // Schedule retry
        task.setStatus(ReportTaskStatus.PENDING);
        task.setNextRetryAt(LocalDateTime.now().plusMinutes(retryDelayMinutes));
        reportTaskRepository.save(task);

        log.info("Report task {} scheduled for retry {} at {}", task.getId(), task.getRetryCount(), task.getNextRetryAt());
      }
    });
  }

  /**
   * Cleanup old completed and failed tasks.
   * Also marks stuck PROCESSING tasks as failed.
   * Runs daily at 3 AM by default.
   */
  @Scheduled(cron = "${report.task.cleanup.cron:0 0 3 * * ?}")
  @Transactional
  public void cleanupOldTasks() {
    // Mark stuck PROCESSING tasks as failed (older than 1 hour)
    LocalDateTime stuckCutoff = LocalDateTime.now().minusHours(1);
    List<ReportTask> stuckTasks = reportTaskRepository.findStuckTasks(ReportTaskStatus.PROCESSING.name(), stuckCutoff);

    for (ReportTask task : stuckTasks) {
      task.setStatus(ReportTaskStatus.FAILED);
      task.setErrorMessage("Task stuck in PROCESSING state - likely OOM or crash");
      task.setUpdatedAt(LocalDateTime.now());
      reportTaskRepository.save(task);

      notifyUserAboutFailure(task);
    }

    if (!stuckTasks.isEmpty()) {
      log.warn("Marked {} stuck PROCESSING tasks as failed", stuckTasks.size());
    }

    // Delete old completed and failed tasks
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cleanupRetentionDays);

    int deletedCount = reportTaskRepository.deleteOldTasks(List.of(ReportTaskStatus.COMPLETED, ReportTaskStatus.FAILED), cutoffDate);

    if (deletedCount > 0) {
      log.info("Cleaned up {} old report tasks older than {}", deletedCount, cutoffDate);
    }
  }

  private void notifyUserAboutFailure(ReportTask task) {
    try {
      telegramBotClient.sendMessage(
          task.getChatId(),
          "Sorry, we were unable to generate your report. Please try again later or contact support if the problem persists."
      );
    } catch (Exception e) {
      log.error("Failed to send failure notification for task {}", task.getId(), e);
    }
  }
}
