package ru.rgasymov.moneymanager.service.xlsx;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
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
    ReflectionTestUtils.setField(xlsxGenerationService, "showEmptyRows", false);
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

  @Test
  void generate_shouldValidateGeneratedContent() throws IOException {
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
    income.setDate(LocalDate.of(2024, 1, 15));
    income.setValue(BigDecimal.valueOf(5000));
    income.setDescription("Monthly salary");
    income.setCategory(incomeCategory);

    var expenseCategory = new OperationCategoryResponseDto();
    expenseCategory.setId(2L);
    expenseCategory.setName("Food");

    var expense = new OperationResponseDto();
    expense.setId(2L);
    expense.setDate(LocalDate.of(2024, 1, 15));
    expense.setValue(BigDecimal.valueOf(100));
    expense.setDescription("Groceries");
    expense.setCategory(expenseCategory);

    var saving = new SavingResponseDto();
    saving.setDate(LocalDate.of(2024, 1, 15));
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
    
    // Parse generated file and validate content
    try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getContentAsByteArray()))) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
      
      var sheet = workbook.getSheetAt(0);
      assertThat(sheet.getSheetName()).isEqualTo("2024");
      
      // Validate category headers
      var categoryRow = sheet.getRow(1);
      assertThat((Object) categoryRow).isNotNull();
      assertThat(categoryRow.getCell(1).getStringCellValue()).isEqualTo("Salary");
      assertThat(categoryRow.getCell(3).getStringCellValue()).isEqualTo("Food");
      
      // Validate data row exists
      var dataRow = sheet.getRow(3);
      assertThat((Object) dataRow).isNotNull();
      
      // Validate date cell
      var dateCell = dataRow.getCell(0);
      assertThat(dateCell.getCellType()).isEqualTo(CellType.NUMERIC);
      
      // Validate income value
      var incomeCell = dataRow.getCell(1);
      assertThat(incomeCell.getCellType()).isEqualTo(CellType.NUMERIC);
      assertThat(incomeCell.getNumericCellValue()).isEqualTo(5000.0);
      
      // Validate expense value
      var expenseCell = dataRow.getCell(3);
      assertThat(expenseCell.getCellType()).isEqualTo(CellType.NUMERIC);
      assertThat(expenseCell.getNumericCellValue()).isEqualTo(100.0);
    }
  }

  @Test
  void generate_shouldCreateSeparateSheetsForDifferentYears() throws IOException {
    var account = new AccountResponseDto();
    account.setId(1L);
    account.setName("Test Account");
    account.setTheme(AccountTheme.LIGHT);
    account.setCurrency("USD");

    var saving2023 = new SavingResponseDto();
    saving2023.setDate(LocalDate.of(2023, 6, 15));
    saving2023.setValue(BigDecimal.valueOf(1000));
    saving2023.setIncomesByCategory(Map.of());
    saving2023.setExpensesByCategory(Map.of());

    var saving2024 = new SavingResponseDto();
    saving2024.setDate(LocalDate.of(2024, 1, 15));
    saving2024.setValue(BigDecimal.valueOf(2000));
    saving2024.setIncomesByCategory(Map.of());
    saving2024.setExpensesByCategory(Map.of());

    var exportData = new FileExportData(
        account,
        List.of(saving2023, saving2024),
        List.of(),
        List.of()
    );

    var template = new ClassPathResource("xlsx/generation-template.xlsx");
    var result = xlsxGenerationService.generate(template, exportData);

    assertThat(result).isNotNull();
    
    // Validate multiple sheets
    try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getContentAsByteArray()))) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
      
      var sheet2023 = workbook.getSheet("2023");
      assertThat(sheet2023).isNotNull();
      
      var sheet2024 = workbook.getSheet("2024");
      assertThat(sheet2024).isNotNull();
    }
  }

  @Test
  void generate_shouldHandleMultipleCategoriesCorrectly() throws IOException {
    var account = new AccountResponseDto();
    account.setId(1L);
    account.setName("Test Account");
    account.setTheme(AccountTheme.LIGHT);
    account.setCurrency("USD");

    var incomeCategory1 = new OperationCategoryResponseDto();
    incomeCategory1.setId(1L);
    incomeCategory1.setName("Salary");

    var incomeCategory2 = new OperationCategoryResponseDto();
    incomeCategory2.setId(2L);
    incomeCategory2.setName("Freelance");

    var expenseCategory1 = new OperationCategoryResponseDto();
    expenseCategory1.setId(3L);
    expenseCategory1.setName("Food");

    var expenseCategory2 = new OperationCategoryResponseDto();
    expenseCategory2.setId(4L);
    expenseCategory2.setName("Transport");

    var saving = new SavingResponseDto();
    saving.setDate(LocalDate.now());
    saving.setValue(BigDecimal.valueOf(1000));
    saving.setIncomesByCategory(Map.of());
    saving.setExpensesByCategory(Map.of());

    var exportData = new FileExportData(
        account,
        List.of(saving),
        List.of(incomeCategory1, incomeCategory2),
        List.of(expenseCategory1, expenseCategory2)
    );

    var template = new ClassPathResource("xlsx/generation-template.xlsx");
    var result = xlsxGenerationService.generate(template, exportData);

    assertThat(result).isNotNull();
    
    // Validate categories are in alphabetical order
    try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getContentAsByteArray()))) {
      var sheet = workbook.getSheetAt(0);
      var categoryRow = sheet.getRow(1);
      
      // Categories should be sorted alphabetically
      var firstIncomeCategory = categoryRow.getCell(1).getStringCellValue();
      var secondIncomeCategory = categoryRow.getCell(2).getStringCellValue();
      
      assertThat(firstIncomeCategory).isLessThan(secondIncomeCategory);
    }
  }
}
