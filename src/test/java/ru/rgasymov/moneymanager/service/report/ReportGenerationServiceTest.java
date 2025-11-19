package ru.rgasymov.moneymanager.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import ru.rgasymov.moneymanager.domain.entity.Expense;
import ru.rgasymov.moneymanager.domain.entity.ExpenseCategory;
import ru.rgasymov.moneymanager.domain.entity.Income;
import ru.rgasymov.moneymanager.domain.entity.IncomeCategory;
import ru.rgasymov.moneymanager.repository.ExpenseRepository;
import ru.rgasymov.moneymanager.repository.IncomeRepository;

@ExtendWith(MockitoExtension.class)
class ReportGenerationServiceTest {

  @Mock
  private ExpenseRepository expenseRepository;

  @Mock
  private IncomeRepository incomeRepository;

  private ReportGenerationService reportGenerationService;

  @BeforeEach
  void setUp() {
    reportGenerationService = new ReportGenerationService(expenseRepository, incomeRepository);
  }

  @Test
  void generateReport_shouldCreateReportFiles() throws IOException {
    var startDate = LocalDate.of(2024, 1, 1);
    var endDate = LocalDate.of(2024, 3, 31);

    var incomeCategory = IncomeCategory.builder().id(1L).name("Salary").build();
    var income = Income.builder()
        .id(1L)
        .date(LocalDate.of(2024, 1, 15))
        .value(BigDecimal.valueOf(5000))
        .category(incomeCategory)
        .build();

    var expenseCategory = ExpenseCategory.builder().id(1L).name("Food").build();
    var expense = Expense.builder()
        .id(1L)
        .date(LocalDate.of(2024, 1, 10))
        .value(BigDecimal.valueOf(100))
        .category(expenseCategory)
        .build();

    when(incomeRepository.findAll(any(Specification.class))).thenReturn(List.of(income));
    when(expenseRepository.findAll(any(Specification.class))).thenReturn(List.of(expense));

    var result = reportGenerationService.generateReport(
        123456L,
        1L,
        startDate,
        endDate,
        null,
        null
    );

    assertThat(result).isNotNull();
    assertThat(result.monthlyChartFile()).isNotNull();
    assertThat(result.expenseChartFile()).isNotNull();
    assertThat(result.incomeChartFile()).isNotNull();

    // Cleanup temp files
    result.monthlyChartFile().delete();
    result.expenseChartFile().delete();
    result.incomeChartFile().delete();
  }

  @Test
  void generateReport_shouldHandleEmptyData() throws IOException {
    var startDate = LocalDate.of(2024, 1, 1);
    var endDate = LocalDate.of(2024, 3, 31);

    when(incomeRepository.findAll(any(Specification.class))).thenReturn(List.of());
    when(expenseRepository.findAll(any(Specification.class))).thenReturn(List.of());

    var result = reportGenerationService.generateReport(
        123456L,
        1L,
        startDate,
        endDate,
        null,
        null
    );

    assertThat(result).isNotNull();
    assertThat(result.monthlyChartFile()).isNotNull();

    // Cleanup
    result.monthlyChartFile().delete();
    result.expenseChartFile().delete();
    result.incomeChartFile().delete();
  }

  @Test
  void generateReport_shouldExcludeCategories() throws IOException {
    var startDate = LocalDate.of(2024, 1, 1);
    var endDate = LocalDate.of(2024, 3, 31);

    var incomeCategory1 = IncomeCategory.builder().id(1L).name("Salary").build();
    var incomeCategory2 = IncomeCategory.builder().id(2L).name("Bonus").build();

    var income1 = Income.builder()
        .id(1L)
        .date(LocalDate.of(2024, 1, 15))
        .value(BigDecimal.valueOf(5000))
        .category(incomeCategory1)
        .build();

    var income2 = Income.builder()
        .id(2L)
        .date(LocalDate.of(2024, 1, 20))
        .value(BigDecimal.valueOf(1000))
        .category(incomeCategory2)
        .build();

    when(incomeRepository.findAll(any(Specification.class))).thenReturn(List.of(income1));
    when(expenseRepository.findAll(any(Specification.class))).thenReturn(List.of());

    var result = reportGenerationService.generateReport(
        123456L,
        1L,
        startDate,
        endDate,
        null,
        "2" // Exclude category 2
    );

    assertThat(result).isNotNull();

    // Cleanup
    result.monthlyChartFile().delete();
    result.expenseChartFile().delete();
    result.incomeChartFile().delete();
  }

  @Test
  void generateReport_shouldHandleMultipleMonths() throws IOException {
    var startDate = LocalDate.of(2024, 1, 1);
    var endDate = LocalDate.of(2024, 6, 30);

    var category = ExpenseCategory.builder().id(1L).name("Food").build();

    var expense1 = Expense.builder()
        .id(1L)
        .date(LocalDate.of(2024, 1, 15))
        .value(BigDecimal.valueOf(100))
        .category(category)
        .build();

    var expense2 = Expense.builder()
        .id(2L)
        .date(LocalDate.of(2024, 3, 15))
        .value(BigDecimal.valueOf(200))
        .category(category)
        .build();

    when(incomeRepository.findAll(any(Specification.class))).thenReturn(List.of());
    when(expenseRepository.findAll(any(Specification.class))).thenReturn(List.of(expense1, expense2));

    var result = reportGenerationService.generateReport(
        123456L,
        1L,
        startDate,
        endDate,
        null,
        null
    );

    assertThat(result).isNotNull();

    // Cleanup
    result.monthlyChartFile().delete();
    result.expenseChartFile().delete();
    result.incomeChartFile().delete();
  }
}
