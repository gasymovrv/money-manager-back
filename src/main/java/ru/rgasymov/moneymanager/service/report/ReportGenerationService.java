package ru.rgasymov.moneymanager.service.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for generating financial reports.
 */
@Service
@Slf4j
public class ReportGenerationService {

  /**
   * Generate report for the specified date range and user.
   * This is a stub implementation that creates a simple text file.
   *
   * @param telegramId the Telegram user ID
   * @param startDate  the start date
   * @param endDate    the end date
   * @return the generated report file
   * @throws IOException if file creation fails
   */
  public File generateReport(Long telegramId, LocalDate startDate, LocalDate endDate) throws IOException {
    log.info("Generating report for user {} from {} to {}", telegramId, startDate, endDate);

    // Create temporary file
    File reportFile = File.createTempFile("report_" + telegramId + "_", ".txt");

    // Write stub content
    try (FileWriter writer = new FileWriter(reportFile)) {
      writer.write("Financial Report\n");
      writer.write("================\n\n");
      writer.write("User ID: " + telegramId + "\n");
      writer.write("Period: " + startDate + " to " + endDate + "\n");
      writer.write("\n");
      writer.write("This is a stub report. Actual implementation pending.\n");
      writer.write("\n");
      writer.write("Generated at: " + java.time.LocalDateTime.now() + "\n");
    }

    log.info("Report generated successfully: {}", reportFile.getAbsolutePath());
    return reportFile;
  }
}
