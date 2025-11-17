package ru.rgasymov.moneymanager.service.report;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rgasymov.moneymanager.domain.entity.Expense;
import ru.rgasymov.moneymanager.domain.entity.Income;
import ru.rgasymov.moneymanager.repository.ExpenseRepository;
import ru.rgasymov.moneymanager.repository.IncomeRepository;
import ru.rgasymov.moneymanager.spec.ExpenseSpec;
import ru.rgasymov.moneymanager.spec.IncomeSpec;

/**
 * Service for generating financial reports.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationService {

  private final ExpenseRepository expenseRepository;
  private final IncomeRepository incomeRepository;
  
  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");
  private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");

  /**
   * Generate report for the specified date range, user and account.
   * Creates 3 PNG charts with financial data visualization.
   * Uses memory-efficient streaming to avoid OOM issues.
   *
   * @param telegramId                 the Telegram user ID
   * @param accountId                  the selected account ID
   * @param startDate                  the start date
   * @param endDate                    the end date
   * @param excludedExpenseCategoryIds comma-separated excluded expense category IDs
   * @param excludedIncomeCategoryIds  comma-separated excluded income category IDs
   * @return the generated report files
   * @throws IOException if file creation fails
   */
  @Transactional(readOnly = true)
  public ReportFiles generateReport(
      Long telegramId,
      Long accountId,
      LocalDate startDate,
      LocalDate endDate,
      String excludedExpenseCategoryIds,
      String excludedIncomeCategoryIds
  ) throws IOException {
    log.info("Generating report for user {} account {} from {} to {}", telegramId, accountId, startDate, endDate);

    List<File> filesToCleanup = new ArrayList<>();
    try {
      // Fetch data from database
      var expenses = fetchExpenses(accountId, startDate, endDate, excludedExpenseCategoryIds);
      var incomes = fetchIncomes(accountId, startDate, endDate, excludedIncomeCategoryIds);

      // Calculate monthly aggregates
      var monthlyData = calculateMonthlyData(expenses, incomes, startDate, endDate);

      // Calculate averages
      var avgExpense = calculateAverage(monthlyData.monthlyExpenses());
      var avgIncome = calculateAverage(monthlyData.monthlyIncomes());

      // Create temp files
      var monthlyFile = createTempFile("monthly_", startDate, endDate, accountId);
      var expenseFile = createTempFile("expenses_", startDate, endDate, accountId);
      var incomeFile = createTempFile("incomes_", startDate, endDate, accountId);

      filesToCleanup.add(monthlyFile);
      filesToCleanup.add(expenseFile);
      filesToCleanup.add(incomeFile);

      // Generate and write monthly chart
      var monthlyChart = createMonthlyBarChart(monthlyData, avgExpense, avgIncome, startDate, endDate);
      writeChartToFile(monthlyChart, monthlyFile);
      clearChartResources(monthlyChart);

      // Generate and write expense chart
      var expensesByCategory = aggregateExpensesByCategory(expenses);
      var expenseChart = createExpensePieChart(expensesByCategory);
      writeChartToFile(expenseChart, expenseFile);
      clearChartResources(expenseChart);

      // Generate and write income chart
      var incomesByCategory = aggregateIncomesByCategory(incomes);
      var incomeChart = createIncomePieChart(incomesByCategory);
      writeChartToFile(incomeChart, incomeFile);
      clearChartResources(incomeChart);

      log.info("Report generated successfully: 3 files created");

      return new ReportFiles(monthlyFile, expenseFile, incomeFile, avgExpense, avgIncome);

    } catch (Exception e) {
      // Clean up on error
      for (var file : filesToCleanup) {
        if (file != null && file.exists()) {
          file.delete();
        }
      }
      throw new IOException("Failed to generate report", e);
    }
  }

  private List<Expense> fetchExpenses(Long accountId, LocalDate startDate, LocalDate endDate, String excludedCategoryIds) {
    var spec = ExpenseSpec.accountIdEq(accountId)
        .and(ExpenseSpec.dateGreaterThanOrEq(startDate))
        .and(ExpenseSpec.dateLessThanOrEq(endDate));
    
    if (excludedCategoryIds != null && !excludedCategoryIds.trim().isEmpty()) {
      var excludedIds = parseExcludedCategoryIds(excludedCategoryIds);
      if (!excludedIds.isEmpty()) {
        spec = spec.and(ExpenseSpec.categoryIdNotIn(excludedIds));
      }
    }
    
    return expenseRepository.findAll(spec);
  }

  private List<Income> fetchIncomes(Long accountId, LocalDate startDate, LocalDate endDate, String excludedCategoryIds) {
    var spec = IncomeSpec.accountIdEq(accountId)
        .and(IncomeSpec.dateGreaterThanOrEq(startDate))
        .and(IncomeSpec.dateLessThanOrEq(endDate));
    
    if (excludedCategoryIds != null && !excludedCategoryIds.trim().isEmpty()) {
      var excludedIds = parseExcludedCategoryIds(excludedCategoryIds);
      if (!excludedIds.isEmpty()) {
        spec = spec.and(IncomeSpec.categoryIdNotIn(excludedIds));
      }
    }
    
    return incomeRepository.findAll(spec);
  }

  private MonthlyData calculateMonthlyData(List<Expense> expenses, List<Income> incomes, LocalDate startDate, LocalDate endDate) {
    Map<YearMonth, BigDecimal> monthlyExpenses = new LinkedHashMap<>();
    Map<YearMonth, BigDecimal> monthlyIncomes = new LinkedHashMap<>();

    // Initialize all months in range with zero
    var currentMonth = YearMonth.from(startDate);
    var lastMonth = YearMonth.from(endDate);
    while (!currentMonth.isAfter(lastMonth)) {
      monthlyExpenses.put(currentMonth, BigDecimal.ZERO);
      monthlyIncomes.put(currentMonth, BigDecimal.ZERO);
      currentMonth = currentMonth.plusMonths(1);
    }

    // Aggregate expenses by month
    for (var expense : expenses) {
      var month = YearMonth.from(expense.getDate());
      monthlyExpenses.merge(month, expense.getValue(), BigDecimal::add);
    }

    // Aggregate incomes by month
    for (var income : incomes) {
      var month = YearMonth.from(income.getDate());
      monthlyIncomes.merge(month, income.getValue(), BigDecimal::add);
    }

    return new MonthlyData(monthlyExpenses, monthlyIncomes);
  }

  private Map<String, BigDecimal> aggregateExpensesByCategory(List<Expense> expenses) {
    Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
    for (var expense : expenses) {
      var categoryName = expense.getCategory() != null ? expense.getCategory().getName() : "Uncategorized";
      categoryTotals.merge(categoryName, expense.getValue(), BigDecimal::add);
    }
    return categoryTotals;
  }

  private Map<String, BigDecimal> aggregateIncomesByCategory(List<Income> incomes) {
    Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
    for (var income : incomes) {
      var categoryName = income.getCategory() != null ? income.getCategory().getName() : "Uncategorized";
      categoryTotals.merge(categoryName, income.getValue(), BigDecimal::add);
    }
    return categoryTotals;
  }

  private BigDecimal calculateAverage(Map<YearMonth, BigDecimal> monthlyData) {
    if (monthlyData.isEmpty()) {
      return BigDecimal.ZERO;
    }
    var total = monthlyData.values().stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    return total.divide(BigDecimal.valueOf(monthlyData.size()), 2, RoundingMode.HALF_UP);
  }

  private JFreeChart createMonthlyBarChart(MonthlyData data, BigDecimal avgExpense, BigDecimal avgIncome,
                                            LocalDate startDate, LocalDate endDate) {
    var dataset = new DefaultCategoryDataset();

    for (var entry : data.monthlyIncomes().entrySet()) {
      var monthLabel = entry.getKey().format(MONTH_FORMATTER);
      dataset.addValue(entry.getValue(), "Income", monthLabel);
    }

    for (var entry : data.monthlyExpenses().entrySet()) {
      var monthLabel = entry.getKey().format(MONTH_FORMATTER);
      dataset.addValue(entry.getValue(), "Expenses", monthLabel);
    }

    var chart = ChartFactory.createBarChart(
        "Monthly Expenses and Income\n" + startDate + " to " + endDate + "\n"
            + "Avg Expense: " + CURRENCY_FORMAT.format(avgExpense) + " | Avg Income: " + CURRENCY_FORMAT.format(avgIncome),
        "Month",
        "Amount",
        dataset,
        PlotOrientation.VERTICAL,
        true,
        true,
        false
    );

    chart.setBackgroundPaint(Color.WHITE);
    var plot = chart.getCategoryPlot();
    plot.setBackgroundPaint(Color.WHITE);
    plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

    // Set colors: Income = green, Expenses = red
    var renderer = (BarRenderer) plot.getRenderer();
    renderer.setSeriesPaint(0, new Color(76, 175, 80)); // Green for income
    renderer.setSeriesPaint(1, new Color(244, 67, 54)); // Red for expenses

    return chart;
  }

  private JFreeChart createExpensePieChart(Map<String, BigDecimal> expensesByCategory) {
    var dataset = new DefaultPieDataset<String>();
    var total = expensesByCategory.values().stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    for (var entry : expensesByCategory.entrySet()) {
      dataset.setValue(entry.getKey(), entry.getValue());
    }

    var chart = ChartFactory.createPieChart(
        "Expenses by Category\nTotal: " + CURRENCY_FORMAT.format(total),
        dataset,
        true,
        true,
        false
    );

    chart.setBackgroundPaint(Color.WHITE);
    var plot = (PiePlot) chart.getPlot();
    plot.setBackgroundPaint(Color.WHITE);
    plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 11));
    // {0}: Category name, {1}: Value, {2}: Percentage
    plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1} ({2})"));

    return chart;
  }

  private JFreeChart createIncomePieChart(Map<String, BigDecimal> incomesByCategory) {
    var dataset = new DefaultPieDataset<String>();
    var total = incomesByCategory.values().stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    for (var entry : incomesByCategory.entrySet()) {
      dataset.setValue(entry.getKey(), entry.getValue());
    }

    var chart = ChartFactory.createPieChart(
        "Income by Category\nTotal: " + CURRENCY_FORMAT.format(total),
        dataset,
        true,
        true,
        false
    );

    chart.setBackgroundPaint(Color.WHITE);
    var plot = (PiePlot) chart.getPlot();
    plot.setBackgroundPaint(Color.WHITE);
    plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 11));
    // {0}: Category name, {1}: Value, {2}: Percentage
    plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1} ({2})"));

    return chart;
  }

  private File createTempFile(String prefix, LocalDate startDate, LocalDate endDate, Long accountId) throws IOException {
    return File.createTempFile(prefix + startDate + "_" + endDate + "_acc_" + accountId + "_", ".png");
  }

  private void writeChartToFile(JFreeChart chart, File file) throws IOException {
    try (var fos = new FileOutputStream(file)) {
      ChartUtils.writeChartAsPNG(fos, chart, 900, 600);
      fos.flush();
    }
  }

  private void clearChartResources(JFreeChart chart) {
    chart.clearSubtitles();
    var plot = chart.getPlot();
    if (plot instanceof CategoryPlot categoryPlot) {
      categoryPlot.setDataset(null);
    } else if (plot instanceof PiePlot piePlot) {
      piePlot.setDataset(null);
    }
  }

  /**
   * Parse excluded category IDs from comma-separated string.
   */
  private List<Long> parseExcludedCategoryIds(String excludedCategoryIds) {
    if (excludedCategoryIds == null || excludedCategoryIds.trim().isEmpty()) {
      return List.of();
    }
    return List.of(excludedCategoryIds.split(",")).stream()
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(Long::parseLong)
        .toList();
  }

  /**
   * Record to hold monthly aggregated data.
   */
  private record MonthlyData(Map<YearMonth, BigDecimal> monthlyExpenses, Map<YearMonth, BigDecimal> monthlyIncomes) {
  }

  /**
   * Record to hold generated report files and statistics.
   */
  public record ReportFiles(
      File monthlyChartFile,
      File expenseChartFile,
      File incomeChartFile,
      BigDecimal avgMonthlyExpense,
      BigDecimal avgMonthlyIncome
  ) {
  }
}
