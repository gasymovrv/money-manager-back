package ru.rgasymov.moneymanager.service.xlsx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import ru.rgasymov.moneymanager.domain.FileExportData;
import ru.rgasymov.moneymanager.domain.dto.response.AccountResponseDto;
import ru.rgasymov.moneymanager.domain.dto.response.OperationCategoryResponseDto;
import ru.rgasymov.moneymanager.domain.dto.response.OperationResponseDto;
import ru.rgasymov.moneymanager.domain.dto.response.SavingResponseDto;
import ru.rgasymov.moneymanager.domain.entity.Account;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.service.UserService;

/**
 * Integration test validating that generated XLSX files can be parsed back correctly.
 * This ensures compatibility between XlsxGenerationService and XlsxParsingService.
 */
@ExtendWith(MockitoExtension.class)
class XlsxRoundTripIntegrationTest {

  @Mock
  private UserService userService;

  @TempDir
  File tempDir;

  private XlsxGenerationService generationService;
  private XlsxParsingService parsingService;
  private Account testAccount;

  @BeforeEach
  void setUp() {
    generationService = new XlsxGenerationService();
    ReflectionTestUtils.setField(generationService, "showEmptyRows", false);
    
    parsingService = new XlsxParsingService(userService);
    
    testAccount = new Account();
    testAccount.setId(1L);
    testAccount.setName("Test Account");
    testAccount.setTheme(AccountTheme.LIGHT);
    testAccount.setCurrency("USD");
    
    var user = new User();
    user.setCurrentAccount(testAccount);
    
    when(userService.getCurrentUser()).thenReturn(user);
  }

  @Test
  void roundTrip_shouldPreserveIncomesAndExpenses() throws Exception {
    // Prepare test data
    var incomeCategory = new OperationCategoryResponseDto();
    incomeCategory.setId(1L);
    incomeCategory.setName("Salary");

    var income = new OperationResponseDto();
    income.setId(1L);
    income.setDate(LocalDate.of(2024, 1, 15));
    income.setValue(BigDecimal.valueOf(5000.50));
    income.setDescription("Monthly salary");
    income.setCategory(incomeCategory);

    var expenseCategory = new OperationCategoryResponseDto();
    expenseCategory.setId(2L);
    expenseCategory.setName("Food");

    var expense = new OperationResponseDto();
    expense.setId(2L);
    expense.setDate(LocalDate.of(2024, 1, 15));
    expense.setValue(BigDecimal.valueOf(100.25));
    expense.setDescription("Groceries");
    expense.setCategory(expenseCategory);

    var saving = new SavingResponseDto();
    saving.setDate(LocalDate.of(2024, 1, 15));
    saving.setValue(BigDecimal.valueOf(4900.25));
    saving.setIncomesByCategory(Map.of("Salary", List.of(income)));
    saving.setExpensesByCategory(Map.of("Food", List.of(expense)));

    var account = new AccountResponseDto();
    account.setId(1L);
    account.setName("Test Account");
    account.setTheme(AccountTheme.LIGHT);
    account.setCurrency("USD");

    var exportData = new FileExportData(
        account,
        List.of(saving),
        List.of(incomeCategory),
        List.of(expenseCategory)
    );

    // Generate XLSX
    var template = new ClassPathResource("xlsx/generation-template.xlsx");
    var generatedResource = generationService.generate(template, exportData);

    // Save to temp file
    var tempFile = new File(tempDir, "test-roundtrip.xlsx");
    try (var in = new ByteArrayInputStream(generatedResource.getContentAsByteArray());
         var out = new FileOutputStream(tempFile)) {
      in.transferTo(out);
    }

    // Parse it back
    var parseResult = parsingService.parse(tempFile);

    // Validate parsed data
    assertThat(parseResult).isNotNull();
    assertThat(parseResult.getIncomes()).hasSize(1);
    assertThat(parseResult.getExpenses()).hasSize(1);
    assertThat(parseResult.getIncomeCategories()).hasSize(1);
    assertThat(parseResult.getExpenseCategories()).hasSize(1);

    // Validate income
    var parsedIncome = parseResult.getIncomes().get(0);
    assertThat(parsedIncome.getValue()).isEqualByComparingTo(BigDecimal.valueOf(5000.50).setScale(2));
    assertThat(parsedIncome.getDate()).isEqualTo(LocalDate.of(2024, 1, 15));
    assertThat(parsedIncome.getAccountId()).isEqualTo(testAccount.getId());
    assertThat(parsedIncome.getCategory().getName()).isEqualTo("Salary");

    // Validate expense
    var parsedExpense = parseResult.getExpenses().get(0);
    assertThat(parsedExpense.getValue()).isEqualByComparingTo(BigDecimal.valueOf(100.25).setScale(2));
    assertThat(parsedExpense.getDate()).isEqualTo(LocalDate.of(2024, 1, 15));
    assertThat(parsedExpense.getAccountId()).isEqualTo(testAccount.getId());
    assertThat(parsedExpense.getCategory().getName()).isEqualTo("Food");
  }

  @Test
  void roundTrip_shouldPreserveMultipleOperationsPerCategory() throws Exception {
    // Prepare test data with multiple operations
    var incomeCategory = new OperationCategoryResponseDto();
    incomeCategory.setId(1L);
    incomeCategory.setName("Salary");

    var income1 = new OperationResponseDto();
    income1.setId(1L);
    income1.setDate(LocalDate.of(2024, 1, 15));
    income1.setValue(BigDecimal.valueOf(2000));
    income1.setDescription("First payment");
    income1.setCategory(incomeCategory);

    var income2 = new OperationResponseDto();
    income2.setId(2L);
    income2.setDate(LocalDate.of(2024, 1, 15));
    income2.setValue(BigDecimal.valueOf(3000));
    income2.setDescription("Second payment");
    income2.setCategory(incomeCategory);

    var saving = new SavingResponseDto();
    saving.setDate(LocalDate.of(2024, 1, 15));
    saving.setValue(BigDecimal.valueOf(5000));
    saving.setIncomesByCategory(Map.of("Salary", List.of(income1, income2)));
    saving.setExpensesByCategory(Map.of());

    var account = new AccountResponseDto();
    account.setId(1L);
    account.setName("Test Account");
    account.setTheme(AccountTheme.LIGHT);
    account.setCurrency("USD");

    var exportData = new FileExportData(
        account,
        List.of(saving),
        List.of(incomeCategory),
        List.of()
    );

    // Generate XLSX
    var template = new ClassPathResource("xlsx/generation-template.xlsx");
    var generatedResource = generationService.generate(template, exportData);

    // Save to temp file
    var tempFile = new File(tempDir, "test-multiple-ops.xlsx");
    try (var in = new ByteArrayInputStream(generatedResource.getContentAsByteArray());
         var out = new FileOutputStream(tempFile)) {
      in.transferTo(out);
    }

    // Parse it back
    var parseResult = parsingService.parse(tempFile);

    // Validate parsed data - should have 2 incomes
    assertThat(parseResult).isNotNull();
    assertThat(parseResult.getIncomes()).hasSize(2);
    
    // Sum of incomes should match
    var totalIncome = parseResult.getIncomes().stream()
        .map(i -> i.getValue())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(totalIncome).isEqualByComparingTo(BigDecimal.valueOf(5000.00).setScale(2));
  }

  @Test
  void roundTrip_shouldPreserveMultipleYears() throws Exception {
    // Prepare test data with multiple years
    var incomeCategory = new OperationCategoryResponseDto();
    incomeCategory.setId(1L);
    incomeCategory.setName("Salary");

    var income2023 = new OperationResponseDto();
    income2023.setId(1L);
    income2023.setDate(LocalDate.of(2023, 12, 15));
    income2023.setValue(BigDecimal.valueOf(1000));
    income2023.setCategory(incomeCategory);

    var income2024 = new OperationResponseDto();
    income2024.setId(2L);
    income2024.setDate(LocalDate.of(2024, 1, 15));
    income2024.setValue(BigDecimal.valueOf(2000));
    income2024.setCategory(incomeCategory);

    var saving2023 = new SavingResponseDto();
    saving2023.setDate(LocalDate.of(2023, 12, 15));
    saving2023.setValue(BigDecimal.valueOf(1000));
    saving2023.setIncomesByCategory(Map.of("Salary", List.of(income2023)));
    saving2023.setExpensesByCategory(Map.of());

    var saving2024 = new SavingResponseDto();
    saving2024.setDate(LocalDate.of(2024, 1, 15));
    saving2024.setValue(BigDecimal.valueOf(2000));
    saving2024.setIncomesByCategory(Map.of("Salary", List.of(income2024)));
    saving2024.setExpensesByCategory(Map.of());

    var account = new AccountResponseDto();
    account.setId(1L);
    account.setName("Test Account");
    account.setTheme(AccountTheme.LIGHT);
    account.setCurrency("USD");

    var exportData = new FileExportData(
        account,
        List.of(saving2023, saving2024),
        List.of(incomeCategory),
        List.of()
    );

    // Generate XLSX
    var template = new ClassPathResource("xlsx/generation-template.xlsx");
    var generatedResource = generationService.generate(template, exportData);

    // Save to temp file
    var tempFile = new File(tempDir, "test-multi-year.xlsx");
    try (var in = new ByteArrayInputStream(generatedResource.getContentAsByteArray());
         var out = new FileOutputStream(tempFile)) {
      in.transferTo(out);
    }

    // Parse it back
    var parseResult = parsingService.parse(tempFile);

    // Validate parsed data - should have incomes from both years
    assertThat(parseResult).isNotNull();
    assertThat(parseResult.getIncomes()).hasSize(2);
    
    var years = parseResult.getIncomes().stream()
        .map(i -> i.getDate().getYear())
        .distinct()
        .sorted()
        .toList();
    
    assertThat(years).containsExactly(2023, 2024);
  }

  @Test
  void roundTrip_shouldPreserveMultipleCategories() throws Exception {
    // Prepare test data with multiple categories
    var incomeCategory1 = new OperationCategoryResponseDto();
    incomeCategory1.setId(1L);
    incomeCategory1.setName("Freelance");

    var incomeCategory2 = new OperationCategoryResponseDto();
    incomeCategory2.setId(2L);
    incomeCategory2.setName("Salary");

    var income1 = new OperationResponseDto();
    income1.setId(1L);
    income1.setDate(LocalDate.of(2024, 1, 15));
    income1.setValue(BigDecimal.valueOf(3000));
    income1.setCategory(incomeCategory1);

    var income2 = new OperationResponseDto();
    income2.setId(2L);
    income2.setDate(LocalDate.of(2024, 1, 15));
    income2.setValue(BigDecimal.valueOf(5000));
    income2.setCategory(incomeCategory2);

    var expenseCategory1 = new OperationCategoryResponseDto();
    expenseCategory1.setId(3L);
    expenseCategory1.setName("Food");

    var expenseCategory2 = new OperationCategoryResponseDto();
    expenseCategory2.setId(4L);
    expenseCategory2.setName("Transport");

    var expense1 = new OperationResponseDto();
    expense1.setId(3L);
    expense1.setDate(LocalDate.of(2024, 1, 15));
    expense1.setValue(BigDecimal.valueOf(100));
    expense1.setCategory(expenseCategory1);

    var expense2 = new OperationResponseDto();
    expense2.setId(4L);
    expense2.setDate(LocalDate.of(2024, 1, 15));
    expense2.setValue(BigDecimal.valueOf(50));
    expense2.setCategory(expenseCategory2);

    var saving = new SavingResponseDto();
    saving.setDate(LocalDate.of(2024, 1, 15));
    saving.setValue(BigDecimal.valueOf(7850));
    saving.setIncomesByCategory(Map.of(
        "Salary", List.of(income2),
        "Freelance", List.of(income1)
    ));
    saving.setExpensesByCategory(Map.of(
        "Food", List.of(expense1),
        "Transport", List.of(expense2)
    ));

    var account = new AccountResponseDto();
    account.setId(1L);
    account.setName("Test Account");
    account.setTheme(AccountTheme.LIGHT);
    account.setCurrency("USD");

    var exportData = new FileExportData(
        account,
        List.of(saving),
        List.of(incomeCategory1, incomeCategory2),
        List.of(expenseCategory1, expenseCategory2)
    );

    // Generate XLSX
    var template = new ClassPathResource("xlsx/generation-template.xlsx");
    var generatedResource = generationService.generate(template, exportData);

    // Save to temp file
    var tempFile = new File(tempDir, "test-multi-categories.xlsx");
    try (var in = new ByteArrayInputStream(generatedResource.getContentAsByteArray());
         var out = new FileOutputStream(tempFile)) {
      in.transferTo(out);
    }

    // Parse it back
    var parseResult = parsingService.parse(tempFile);

    // Validate parsed data
    assertThat(parseResult).isNotNull();
    assertThat(parseResult.getIncomes()).hasSize(2);
    assertThat(parseResult.getExpenses()).hasSize(2);
    assertThat(parseResult.getIncomeCategories()).hasSize(2);
    assertThat(parseResult.getExpenseCategories()).hasSize(2);

    // Validate categories
    var incomeCategoryNames = parseResult.getIncomeCategories().stream()
        .map(c -> c.getName())
        .sorted()
        .toList();
    assertThat(incomeCategoryNames).containsExactly("Freelance", "Salary");

    var expenseCategoryNames = parseResult.getExpenseCategories().stream()
        .map(c -> c.getName())
        .sorted()
        .toList();
    assertThat(expenseCategoryNames).containsExactly("Food", "Transport");
  }
}
