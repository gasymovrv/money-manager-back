package ru.rgasymov.moneymanager.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rgasymov.moneymanager.domain.entity.ReportTask;
import ru.rgasymov.moneymanager.domain.entity.ReportTask.ReportTaskStatus;

/**
 * Repository for ReportTask entity.
 */
@Repository
public interface ReportTaskRepository extends JpaRepository<ReportTask, Long> {

  /**
   * Find all pending tasks that are ready for processing.
   *
   * @param now the current time
   * @return list of tasks ready for processing
   */
  List<ReportTask> findByStatusAndNextRetryAtLessThanEqual(
      ReportTaskStatus status,
      LocalDateTime now
  );

  /**
   * Find all pending tasks.
   *
   * @param status the status
   * @return list of pending tasks
   */
  List<ReportTask> findByStatus(ReportTaskStatus status);
}
