package ru.rgasymov.moneymanager.service.xlsx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rgasymov.moneymanager.domain.entity.Account;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.service.UserService;

@ExtendWith(MockitoExtension.class)
class XlsxParsingServiceTest {

  @Mock
  private UserService userService;

  @TempDir
  File tempDir;

  private XlsxParsingService xlsxParsingService;
  private Account testAccount;

  @BeforeEach
  void setUp() {
    xlsxParsingService = new XlsxParsingService(userService);
    
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
  void parse_shouldParseIncomesAndExpenses() throws Exception {
    var file = createTestXlsxFile();
    
    var result = xlsxParsingService.parse(file);
    
    assertThat(result).isNotNull();
    assertThat(result.getIncomes()).hasSize(2);
    assertThat(result.getExpenses()).hasSize(2);
    assertThat(result.getIncomeCategories()).hasSize(1);
    assertThat(result.getExpenseCategories()).hasSize(1);
    
    // Verify income data
    var income = result.getIncomes().get(0);
    assertThat(income.getValue()).isEqualByComparingTo(BigDecimal.valueOf(1000.00).setScale(2));
    assertThat(income.getDate()).isEqualTo(LocalDate.now());
    assertThat(income.getAccountId()).isEqualTo(testAccount.getId());
    assertThat(income.getCategory().getName()).isEqualTo("Salary");
    
    // Verify expense data
    var expense = result.getExpenses().get(0);
    assertThat(expense.getValue()).isEqualByComparingTo(BigDecimal.valueOf(50.00).setScale(2));
    assertThat(expense.getDate()).isEqualTo(LocalDate.now());
    assertThat(expense.getAccountId()).isEqualTo(testAccount.getId());
    assertThat(expense.getCategory().getName()).isEqualTo("Food");
  }

  @Test
  void parse_shouldParseCommentWithMultipleOperations() throws Exception {
    var file = createTestXlsxFileWithComments();
    
    var result = xlsxParsingService.parse(file);
    
    assertThat(result).isNotNull();
    assertThat(result.getIncomes()).hasSize(2); // One cell with comment creates 2 operations
    
    var income1 = result.getIncomes().get(0);
    var income2 = result.getIncomes().get(1);
    
    assertThat(income1.getValue()).isEqualByComparingTo(BigDecimal.valueOf(500.00).setScale(2));
    assertThat(income1.getDescription()).isEqualTo("Bonus");
    
    assertThat(income2.getValue()).isEqualByComparingTo(BigDecimal.valueOf(500.00).setScale(2));
    assertThat(income2.getDescription()).isEqualTo("Side job");
  }

  @Test
  void parse_shouldParsePreviousSavings() throws Exception {
    var file = createTestXlsxFileWithPreviousSavings();
    
    var result = xlsxParsingService.parse(file);
    
    assertThat(result).isNotNull();
    assertThat(result.getPreviousSavings()).isEqualByComparingTo(BigDecimal.valueOf(5000.00).setScale(2));
    assertThat(result.getPreviousSavingsDate()).isNotNull();
  }

  @Test
  void parse_shouldHandleMultipleSheets() throws Exception {
    var file = createTestXlsxFileWithMultipleSheets();
    
    var result = xlsxParsingService.parse(file);
    
    assertThat(result).isNotNull();
    // Data from both sheets should be combined
    assertThat(result.getIncomes()).hasSizeGreaterThanOrEqualTo(2);
    assertThat(result.getExpenses()).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  void parse_shouldSkipEmptyRows() throws Exception {
    var file = createTestXlsxFileWithEmptyRows();
    
    var result = xlsxParsingService.parse(file);
    
    assertThat(result).isNotNull();
    assertThat(result.getIncomes()).hasSize(1); // Only non-empty rows
  }

  @Test
  void parse_shouldHandlePlannedOperations() throws Exception {
    var file = createTestXlsxFileWithFutureDates();
    
    var result = xlsxParsingService.parse(file);
    
    assertThat(result).isNotNull();
    var plannedIncome = result.getIncomes().stream()
        .filter(i -> i.getDate().isAfter(LocalDate.now()))
        .findFirst()
        .orElseThrow();
    
    assertThat(plannedIncome.getIsPlanned()).isTrue();
  }

  private File createTestXlsxFile() throws IOException {
    var file = new File(tempDir, "test.xlsx");
    try (var workbook = new XSSFWorkbook(); var out = new FileOutputStream(file)) {
      var sheet = workbook.createSheet("2024");
      
      // Header row
      var headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("Date");
      headerRow.createCell(1).setCellValue("Incomes");
      headerRow.createCell(2).setCellValue("Incomes sum");
      headerRow.createCell(3).setCellValue("Expenses");
      headerRow.createCell(4).setCellValue("Expenses sum");
      headerRow.createCell(5).setCellValue("Savings");
      
      // Category row
      var categoryRow = sheet.createRow(1);
      categoryRow.createCell(1).setCellValue("Salary");
      categoryRow.createCell(2).setCellValue("Incomes sum");
      categoryRow.createCell(3).setCellValue("Food");
      categoryRow.createCell(4).setCellValue("Expenses sum");
      categoryRow.createCell(5).setCellValue("Savings");
      
      // Previous savings row
      var prevSavingsRow = sheet.createRow(2);
      prevSavingsRow.createCell(5, CellType.NUMERIC).setCellValue(0.0); // Set to 0 to avoid null
      
      // Data rows
      var dataRow1 = sheet.createRow(3);
      var dateCell1 = dataRow1.createCell(0, CellType.NUMERIC);
      dateCell1.setCellValue(LocalDateTime.now());
      dataRow1.createCell(1, CellType.NUMERIC).setCellValue(1000.00);
      dataRow1.createCell(3, CellType.NUMERIC).setCellValue(50.00);
      
      var dataRow2 = sheet.createRow(4);
      var dateCell2 = dataRow2.createCell(0, CellType.NUMERIC);
      dateCell2.setCellValue(LocalDateTime.now().minusDays(1));
      dataRow2.createCell(1, CellType.NUMERIC).setCellValue(2000.00);
      dataRow2.createCell(3, CellType.NUMERIC).setCellValue(100.00);
      
      workbook.write(out);
    }
    return file;
  }

  private File createTestXlsxFileWithComments() throws IOException {
    var file = new File(tempDir, "test-comments.xlsx");
    try (var workbook = new XSSFWorkbook(); var out = new FileOutputStream(file)) {
      var sheet = workbook.createSheet("2024");
      
      // Header row
      var headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("Date");
      headerRow.createCell(1).setCellValue("Incomes");
      headerRow.createCell(2).setCellValue("Incomes sum");
      headerRow.createCell(3).setCellValue("Expenses");
      headerRow.createCell(4).setCellValue("Expenses sum");
      headerRow.createCell(5).setCellValue("Savings");
      
      // Category row
      var categoryRow = sheet.createRow(1);
      categoryRow.createCell(1).setCellValue("Salary");
      categoryRow.createCell(2).setCellValue("Incomes sum");
      categoryRow.createCell(3).setCellValue("Food");
      categoryRow.createCell(4).setCellValue("Expenses sum");
      categoryRow.createCell(5).setCellValue("Savings");
      
      // Previous savings row
      var prevSavingsRow = sheet.createRow(2);
      prevSavingsRow.createCell(5, CellType.NUMERIC).setCellValue(0.0);
      
      // Data row with comment
      var dataRow = sheet.createRow(3);
      var dateCell = dataRow.createCell(0, CellType.NUMERIC);
      dateCell.setCellValue(LocalDateTime.now());
      var incomeCell = dataRow.createCell(1, CellType.NUMERIC);
      incomeCell.setCellValue(1000.00);
      
      // Add comment in the format: "Description1 (value1); Description2 (value2);"
      var drawing = sheet.createDrawingPatriarch();
      var anchor = workbook.getCreationHelper().createClientAnchor();
      var comment = drawing.createCellComment(anchor);
      comment.setString(workbook.getCreationHelper()
          .createRichTextString("Bonus (500.00); Side job (500.00);"));
      incomeCell.setCellComment(comment);
      
      workbook.write(out);
    }
    return file;
  }

  private File createTestXlsxFileWithPreviousSavings() throws IOException {
    var file = new File(tempDir, "test-prev-savings.xlsx");
    try (var workbook = new XSSFWorkbook(); var out = new FileOutputStream(file)) {
      var sheet = workbook.createSheet("2024");
      
      // Header row
      var headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("Date");
      headerRow.createCell(1).setCellValue("Incomes");
      headerRow.createCell(2).setCellValue("Incomes sum");
      headerRow.createCell(3).setCellValue("Expenses");
      headerRow.createCell(4).setCellValue("Expenses sum");
      headerRow.createCell(5).setCellValue("Savings");
      
      // Category row
      var categoryRow = sheet.createRow(1);
      categoryRow.createCell(1).setCellValue("Salary");
      categoryRow.createCell(2).setCellValue("Incomes sum");
      categoryRow.createCell(3).setCellValue("Food");
      categoryRow.createCell(4).setCellValue("Expenses sum");
      categoryRow.createCell(5).setCellValue("Savings");
      
      // Previous savings row
      var prevSavingsRow = sheet.createRow(2);
      prevSavingsRow.createCell(5, CellType.NUMERIC).setCellValue(5000.00);
      
      // Data row
      var dataRow = sheet.createRow(3);
      var dateCell = dataRow.createCell(0, CellType.NUMERIC);
      dateCell.setCellValue(LocalDateTime.now());
      dataRow.createCell(1, CellType.NUMERIC).setCellValue(1000.00);
      
      workbook.write(out);
    }
    return file;
  }

  private File createTestXlsxFileWithMultipleSheets() throws IOException {
    var file = new File(tempDir, "test-multiple-sheets.xlsx");
    try (var workbook = new XSSFWorkbook(); var out = new FileOutputStream(file)) {
      // Create sheet for 2023
      createSheetWithData(workbook, "2023", LocalDateTime.now().minusYears(1));
      // Create sheet for 2024
      createSheetWithData(workbook, "2024", LocalDateTime.now());
      
      workbook.write(out);
    }
    return file;
  }

  private void createSheetWithData(XSSFWorkbook workbook, String sheetName, LocalDateTime date) {
    var sheet = workbook.createSheet(sheetName);
    
    // Header row
    var headerRow = sheet.createRow(0);
    headerRow.createCell(0).setCellValue("Date");
    headerRow.createCell(1).setCellValue("Incomes");
    headerRow.createCell(2).setCellValue("Incomes sum");
    headerRow.createCell(3).setCellValue("Expenses");
    headerRow.createCell(4).setCellValue("Expenses sum");
    headerRow.createCell(5).setCellValue("Savings");
    
    // Category row
    var categoryRow = sheet.createRow(1);
    categoryRow.createCell(1).setCellValue("Salary");
    categoryRow.createCell(2).setCellValue("Incomes sum");
    categoryRow.createCell(3).setCellValue("Food");
    categoryRow.createCell(4).setCellValue("Expenses sum");
    categoryRow.createCell(5).setCellValue("Savings");
    
    // Previous savings row
    var prevSavingsRow = sheet.createRow(2);
    prevSavingsRow.createCell(5, CellType.NUMERIC).setCellValue(0.0);
    
    // Data row
    var dataRow = sheet.createRow(3);
    var dateCell = dataRow.createCell(0, CellType.NUMERIC);
    dateCell.setCellValue(date);
    dataRow.createCell(1, CellType.NUMERIC).setCellValue(1000.00);
    dataRow.createCell(3, CellType.NUMERIC).setCellValue(50.00);
  }

  private File createTestXlsxFileWithEmptyRows() throws IOException {
    var file = new File(tempDir, "test-empty-rows.xlsx");
    try (var workbook = new XSSFWorkbook(); var out = new FileOutputStream(file)) {
      var sheet = workbook.createSheet("2024");
      
      // Header row
      var headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("Date");
      headerRow.createCell(1).setCellValue("Incomes");
      headerRow.createCell(2).setCellValue("Incomes sum");
      headerRow.createCell(3).setCellValue("Expenses");
      headerRow.createCell(4).setCellValue("Expenses sum");
      headerRow.createCell(5).setCellValue("Savings");
      
      // Category row
      var categoryRow = sheet.createRow(1);
      categoryRow.createCell(1).setCellValue("Salary");
      categoryRow.createCell(2).setCellValue("Incomes sum");
      categoryRow.createCell(3).setCellValue("Food");
      categoryRow.createCell(4).setCellValue("Expenses sum");
      categoryRow.createCell(5).setCellValue("Savings");
      
      // Previous savings row
      var prevSavingsRow = sheet.createRow(2);
      prevSavingsRow.createCell(5, CellType.NUMERIC).setCellValue(0.0);
      
      // Data row with data
      var dataRow1 = sheet.createRow(3);
      var dateCell1 = dataRow1.createCell(0, CellType.NUMERIC);
      dateCell1.setCellValue(LocalDateTime.now());
      dataRow1.createCell(1, CellType.NUMERIC).setCellValue(1000.00);
      
      // Empty row (null row, don't create it)
      
      // Row with no numeric data in first cell to trigger break
      var dataRow3 = sheet.createRow(5);
      dataRow3.createCell(0, CellType.STRING).setCellValue("Invalid");
      
      workbook.write(out);
    }
    return file;
  }

  private File createTestXlsxFileWithFutureDates() throws IOException {
    var file = new File(tempDir, "test-future-dates.xlsx");
    try (var workbook = new XSSFWorkbook(); var out = new FileOutputStream(file)) {
      var sheet = workbook.createSheet("2024");
      
      // Header row
      var headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("Date");
      headerRow.createCell(1).setCellValue("Incomes");
      headerRow.createCell(2).setCellValue("Incomes sum");
      headerRow.createCell(3).setCellValue("Expenses");
      headerRow.createCell(4).setCellValue("Expenses sum");
      headerRow.createCell(5).setCellValue("Savings");
      
      // Category row
      var categoryRow = sheet.createRow(1);
      categoryRow.createCell(1).setCellValue("Salary");
      categoryRow.createCell(2).setCellValue("Incomes sum");
      categoryRow.createCell(3).setCellValue("Food");
      categoryRow.createCell(4).setCellValue("Expenses sum");
      categoryRow.createCell(5).setCellValue("Savings");
      
      // Previous savings row
      var prevSavingsRow = sheet.createRow(2);
      prevSavingsRow.createCell(5, CellType.NUMERIC).setCellValue(0.0);
      
      // Data row with future date
      var dataRow = sheet.createRow(3);
      var dateCell = dataRow.createCell(0, CellType.NUMERIC);
      dateCell.setCellValue(LocalDateTime.now().plusDays(30));
      dataRow.createCell(1, CellType.NUMERIC).setCellValue(1000.00);
      
      workbook.write(out);
    }
    return file;
  }
}
