package ru.rgasymov.moneymanager.service.report;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Service;

/**
 * Service for generating financial reports.
 */
@Service
@Slf4j
public class ReportGenerationService {

  /**
   * Generate report for the specified date range and user.
   * Creates a PNG chart with financial data visualization.
   * Uses memory-efficient streaming to avoid OOM issues.
   *
   * @param telegramId the Telegram user ID
   * @param startDate  the start date
   * @param endDate    the end date
   * @return the generated report file
   * @throws IOException if file creation fails
   */
  public File generateReport(Long telegramId, LocalDate startDate, LocalDate endDate) throws IOException {
    log.info("Generating report for user {} from {} to {}", telegramId, startDate, endDate);

    File reportFile = null;
    try {
      // Create temporary file
      reportFile = File.createTempFile("report_" + startDate + "_" + endDate + "_", ".png");

      // Generate chart (stub data - replace with actual DB queries)
      JFreeChart chart = createFinancialChart(telegramId, startDate, endDate);

      // Write chart to file with streaming to minimize memory usage
      try (FileOutputStream fos = new FileOutputStream(reportFile)) {
        ChartUtils.writeChartAsPNG(fos, chart, 800, 600);
        fos.flush();
      }

      // Clear chart resources immediately
      chart.clearSubtitles();
      if (chart.getPlot() instanceof CategoryPlot plot) {
        plot.setDataset(null);
      }

      log.info("Report generated successfully: {} (size: {} KB)",
          reportFile.getAbsolutePath(),
          reportFile.length() / 1024);

      return reportFile;

    } catch (Exception e) {
      // Clean up on error
      if (reportFile != null && reportFile.exists()) {
        reportFile.delete();
      }
      throw new IOException("Failed to generate report", e);
    }
  }

  /**
   * Create financial chart with stub data.
   * TODO: Replace with actual data from database.
   */
  private JFreeChart createFinancialChart(Long telegramId, LocalDate startDate, LocalDate endDate) {
    // Create dataset with stub data
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    // TODO: Replace with actual DB queries
    // Example stub data
    dataset.addValue(1000, "Income", "Week 1");
    dataset.addValue(1500, "Income", "Week 2");
    dataset.addValue(1200, "Income", "Week 3");
    dataset.addValue(1800, "Income", "Week 4");

    dataset.addValue(800, "Expenses", "Week 1");
    dataset.addValue(900, "Expenses", "Week 2");
    dataset.addValue(1100, "Expenses", "Week 3");
    dataset.addValue(950, "Expenses", "Week 4");

    // Create chart
    JFreeChart chart = ChartFactory.createBarChart(
        "Financial Report: " + startDate + " to " + endDate,
        "Period",
        "Amount",
        dataset,
        PlotOrientation.VERTICAL,
        true,
        true,
        false
    );

    // Customize appearance
    chart.setBackgroundPaint(Color.WHITE);
    CategoryPlot plot = chart.getCategoryPlot();
    plot.setBackgroundPaint(Color.LIGHT_GRAY);
    plot.setRangeGridlinePaint(Color.WHITE);

    return chart;
  }
}
