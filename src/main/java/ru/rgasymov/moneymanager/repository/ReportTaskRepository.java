package ru.rgasymov.moneymanager.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rgasymov.moneymanager.domain.entity.ReportTask;
import ru.rgasymov.moneymanager.domain.entity.ReportTask.ReportTaskStatus;

/**
 * Repository for ReportTask entity.
 */
@Repository
public interface ReportTaskRepository extends JpaRepository<ReportTask, Long> {

  /**
   * Find all pending tasks that are ready for processing with pessimistic lock.
   * Uses SELECT FOR UPDATE SKIP LOCKED to prevent concurrent processing.
   *
   * @param status the task status
   * @param now the current time
   * @return list of tasks ready for processing
   */
  @Query(value = "SELECT * FROM report_tasks WHERE status = :status AND next_retry_at <= :now FOR UPDATE SKIP LOCKED", nativeQuery = true)
  List<ReportTask> findTasksForProcessing(
      @Param("status") String status,
      @Param("now") LocalDateTime now
  );

  /**
   * Delete old completed or failed tasks.
   *
   * @param statuses the statuses to delete
   * @param olderThan delete tasks older than this date
   * @return number of deleted tasks
   */
  @Modifying
  @Query("DELETE FROM ReportTask t WHERE t.status IN :statuses AND t.updatedAt < :olderThan")
  int deleteOldTasks(
      @Param("statuses") List<ReportTaskStatus> statuses,
      @Param("olderThan") LocalDateTime olderThan
  );
}
