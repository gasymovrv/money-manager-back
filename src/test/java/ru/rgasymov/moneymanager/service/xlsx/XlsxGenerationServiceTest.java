package ru.rgasymov.moneymanager.service.xlsx;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import ru.rgasymov.moneymanager.domain.FileExportData;
import ru.rgasymov.moneymanager.domain.dto.response.AccountResponseDto;
import ru.rgasymov.moneymanager.domain.dto.response.OperationCategoryResponseDto;
import ru.rgasymov.moneymanager.domain.dto.response.OperationResponseDto;
import ru.rgasymov.moneymanager.domain.dto.response.SavingResponseDto;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;

class XlsxGenerationServiceTest {

  private XlsxGenerationService xlsxGenerationService;

  @BeforeEach
  void setUp() {
    xlsxGenerationService = new XlsxGenerationService();
  }

  @Test
  void generate_shouldCreateExcelFile_whenValidData() throws IOException {
    var account = new AccountResponseDto();
    account.setId(1L);
    account.setName("Test Account");
    account.setTheme(AccountTheme.LIGHT);
    account.setCurrency("USD");

    var saving = new SavingResponseDto();
    saving.setDate(LocalDate.now());
    saving.setValue(BigDecimal.valueOf(1000));
    saving.setIncomesByCategory(Map.of());
    saving.setExpensesByCategory(Map.of());

    var exportData = new FileExportData(
        account,
        List.of(saving),
        List.of(),
        List.of()
    );

    var template = new ClassPathResource("xlsx/generation-template.xlsx");
    var result = xlsxGenerationService.generate(template, exportData);

    assertThat(result).isNotNull();
    assertThat(result.contentLength()).isGreaterThan(0);
  }

  @Test
  void generate_shouldHandleEmptyData() throws IOException {
    var account = new AccountResponseDto();
    account.setId(1L);
    account.setName("Test Account");
    account.setTheme(AccountTheme.LIGHT);
    account.setCurrency("USD");

    var exportData = new FileExportData(
        account,
        List.of(),
        List.of(),
        List.of()
    );

    var template = new ClassPathResource("xlsx/generation-template.xlsx");
    var result = xlsxGenerationService.generate(template, exportData);

    assertThat(result).isNotNull();
  }

  @Test
  void generate_shouldIncludeOperations() throws IOException {
    var account = new AccountResponseDto();
    account.setId(1L);
    account.setName("Test Account");
    account.setTheme(AccountTheme.LIGHT);
    account.setCurrency("USD");

    var incomeCategory = new OperationCategoryResponseDto();
    incomeCategory.setId(1L);
    incomeCategory.setName("Salary");

    var income = new OperationResponseDto();
    income.setId(1L);
    income.setDate(LocalDate.now());
    income.setValue(BigDecimal.valueOf(5000));
    income.setDescription("Monthly salary");
    income.setCategory(incomeCategory);

    var expenseCategory = new OperationCategoryResponseDto();
    expenseCategory.setId(1L);
    expenseCategory.setName("Food");

    var expense = new OperationResponseDto();
    expense.setId(1L);
    expense.setDate(LocalDate.now());
    expense.setValue(BigDecimal.valueOf(100));
    expense.setDescription("Groceries");
    expense.setCategory(expenseCategory);

    var saving = new SavingResponseDto();
    saving.setDate(LocalDate.now());
    saving.setValue(BigDecimal.valueOf(4900));
    saving.setIncomesByCategory(Map.of("Salary", List.of(income)));
    saving.setExpensesByCategory(Map.of("Food", List.of(expense)));

    var exportData = new FileExportData(
        account,
        List.of(saving),
        List.of(incomeCategory),
        List.of(expenseCategory)
    );

    var template = new ClassPathResource("xlsx/generation-template.xlsx");
    var result = xlsxGenerationService.generate(template, exportData);

    assertThat(result).isNotNull();
    assertThat(result.contentLength()).isGreaterThan(0);
  }

  @Test
  void generate_shouldHandleMultipleSavings() throws IOException {
    var account = new AccountResponseDto();
    account.setId(1L);
    account.setName("Test Account");
    account.setTheme(AccountTheme.LIGHT);
    account.setCurrency("USD");

    var saving1 = new SavingResponseDto();
    saving1.setDate(LocalDate.now().minusDays(2));
    saving1.setValue(BigDecimal.valueOf(1000));
    saving1.setIncomesByCategory(Map.of());
    saving1.setExpensesByCategory(Map.of());

    var saving2 = new SavingResponseDto();
    saving2.setDate(LocalDate.now().minusDays(1));
    saving2.setValue(BigDecimal.valueOf(1500));
    saving2.setIncomesByCategory(Map.of());
    saving2.setExpensesByCategory(Map.of());

    var exportData = new FileExportData(
        account,
        List.of(saving1, saving2),
        List.of(),
        List.of()
    );

    var template = new ClassPathResource("xlsx/generation-template.xlsx");
    var result = xlsxGenerationService.generate(template, exportData);

    assertThat(result).isNotNull();
    assertThat(result.contentLength()).isGreaterThan(0);
  }
}
