package ru.rgasymov.moneymanager.service.xlsx;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import ru.rgasymov.moneymanager.domain.FileImportResult;
import ru.rgasymov.moneymanager.domain.entity.Expense;
import ru.rgasymov.moneymanager.domain.entity.ExpenseCategory;
import ru.rgasymov.moneymanager.domain.entity.Income;
import ru.rgasymov.moneymanager.domain.entity.IncomeCategory;
import ru.rgasymov.moneymanager.service.UserService;

@Service
@RequiredArgsConstructor
@Slf4j
public class XlsxParsingService {

  /**
   * Index of the row with category names.
   */
  private static final int CATEGORIES_ROW = 1;

  /**
   * Index of the first row with income and expense.
   */
  private static final int START_ROW = 3;

  /**
   * Index of the first income column.
   */
  private static final int START_COLUMN = 1;

  /**
   * Column names.
   */
  private static final String INCOMES_SUM_COLUMN_NAME = "Incomes sum";
  private static final String EXPENSES_SUM_COLUMN_NAME = "Expenses sum";

  /**
   * Index of the previous savings row.
   */
  private static final int PREVIOUS_SAVINGS_ROW = 2;

  /**
   * Savings column name.
   */
  private static final String SAVINGS_COLUMN_NAME = "Savings";

  /**
   * Pattern to parse complex comment
   * created for several operations with the same date.
   */
  private static final Pattern MULTI_OPERATIONS_COMMENT_PATTERN =
      Pattern.compile("(.*?)\\(?([0-9.]+)\\)?;");

  private static final int SCALE = 2;

  private final UserService userService;

  public FileImportResult parse(File file) throws IOException, InvalidFormatException {
    try (var workBook = new XSSFWorkbook(file)) {
      var sheetIterator = workBook.sheetIterator();

      FileImportResult result = null;
      SortedSet<XSSFSheet> sortedSheets =
          new TreeSet<>(Comparator.comparing(XSSFSheet::getSheetName));

      while (sheetIterator.hasNext()) {
        var sheet = (XSSFSheet) sheetIterator.next();
        sortedSheets.add(sheet);

        var incCategoriesRow = sheet.getRow(CATEGORIES_ROW);
        var expCategoriesRow = sheet.getRow(CATEGORIES_ROW);

        FileImportResult tempResult = extractData(sheet, incCategoriesRow, expCategoriesRow);
        if (result != null) {
          result.add(tempResult);
        } else {
          result = tempResult;
        }
      }

      addPrevSavings(result, sortedSheets);
      return result;
    }
  }

  private FileImportResult extractData(XSSFSheet sheet,
                                       XSSFRow incCategoriesRow,
                                       XSSFRow expCategoriesRow) {
    var currentAccount = userService.getCurrentUser().getCurrentAccount();
    var incomes = new ArrayList<Income>();
    var expenses = new ArrayList<Expense>();
    var incomeCategories = new HashMap<Integer, IncomeCategory>();
    var expenseCategories = new HashMap<Integer, ExpenseCategory>();

    //Find all categories
    Integer incomeLastCol = findIncomeCategories(incCategoriesRow, incomeCategories);
    findExpenseCategories(expCategoriesRow, expenseCategories, incomeLastCol);

    //Iterate by data rows
    for (int i = START_ROW; i <= sheet.getLastRowNum(); i++) {
      var row = sheet.getRow(i);
      if (row == null) {
        continue;
      }
      var firstCell = row.getCell(0);

      if (CellType.NUMERIC != firstCell.getCellType()) {
        break;
      }
      LocalDateTime firstCellValue = firstCell.getLocalDateTimeCellValue();
      if (firstCellValue == null) {
        continue;
      }

      var date = firstCellValue.toLocalDate();

      //Iterate by cells in row
      for (int j = START_COLUMN; j <= row.getLastCellNum(); j++) {
        var cell = row.getCell(j);
        if (cell == null
            || CellType.NUMERIC != cell.getCellType()
            || cell.getCellStyle().getFillBackgroundXSSFColor() != null) {
          continue;
        }

        var columnIndex = cell.getColumnIndex();
        var cellValue = cell.getNumericCellValue();
        String cellComment = null;
        if (cell.getCellComment() != null
            && cell.getCellComment().getString() != null) {
          cellComment = cell.getCellComment().getString().toString();
        }

        var incomeCategory = incomeCategories.get(columnIndex);
        var expenseCategory = expenseCategories.get(columnIndex);

        var today = LocalDate.now();
        if (incomeCategory != null && cellValue != 0) {
          var incomesPerDay = buildOperationDrafts(cellValue, cellComment)
              .stream()
              .map(od -> Income.builder()
                  .date(date)
                  .value(od.value())
                  .isPlanned(date.isAfter(today))
                  .category(incomeCategory)
                  .description(od.comment())
                  .accountId(currentAccount.getId())
                  .build())
              .toList();
          incomes.addAll(incomesPerDay);
        } else if (expenseCategory != null && cellValue != 0) {
          var expensesPerDay = buildOperationDrafts(cellValue, cellComment)
              .stream()
              .map(od -> Expense.builder()
                  .date(date)
                  .value(od.value())
                  .isPlanned(date.isAfter(today))
                  .category(expenseCategory)
                  .description(od.comment())
                  .accountId(currentAccount.getId())
                  .build())
              .toList();
          expenses.addAll(expensesPerDay);
        }
      }
    }

    return new FileImportResult(
        incomes,
        expenses,
        new HashSet<>(incomeCategories.values()),
        new HashSet<>(expenseCategories.values())
    );
  }

  private void findExpenseCategories(XSSFRow expCategoriesRow,
                                     HashMap<Integer, ExpenseCategory> expenseCategories,
                                     Integer incomeLastCol) {
    if (incomeLastCol == null) {
      return;
    }
    var currentAccount = userService.getCurrentUser().getCurrentAccount();

    for (int i = incomeLastCol + 1;
         i <= expCategoriesRow.getLastCellNum();
         i++) {
      var cell = expCategoriesRow.getCell(i);
      var cellValue = cell.getStringCellValue();

      if (cellValue.equals(EXPENSES_SUM_COLUMN_NAME)) {
        return;
      } else {
        var expenseCategory = ExpenseCategory.builder()
            .name(cellValue)
            .accountId(currentAccount.getId())
            .build();
        expenseCategories.put(cell.getColumnIndex(), expenseCategory);
      }
    }
  }

  private Integer findIncomeCategories(XSSFRow incCategoriesRow,
                                       HashMap<Integer, IncomeCategory> incomeCategories) {
    var currentAccount = userService.getCurrentUser().getCurrentAccount();

    for (int i = START_COLUMN; i <= incCategoriesRow.getLastCellNum(); i++) {
      var cell = incCategoriesRow.getCell(i);
      var cellValue = cell.getStringCellValue();

      if (cellValue.equals(INCOMES_SUM_COLUMN_NAME)) {
        return cell.getColumnIndex();
      } else {
        var incomeCategory = IncomeCategory.builder()
            .name(cellValue)
            .accountId(currentAccount.getId())
            .build();
        incomeCategories.put(cell.getColumnIndex(), incomeCategory);
      }
    }
    return null;
  }

  private void addPrevSavings(FileImportResult result,
                              SortedSet<XSSFSheet> sortedSheets) {
    XSSFSheet oldest = sortedSheets.first();
    XSSFRow headRow = oldest.getRow(0);

    for (Cell cell : headRow) {
      if (cell != null
          && cell.getCellType() == CellType.STRING
          && cell.getStringCellValue().equals(SAVINGS_COLUMN_NAME)) {

        //Get previous savings cell and its value
        XSSFCell prevSavingsCell =
            oldest.getRow(PREVIOUS_SAVINGS_ROW).getCell(cell.getColumnIndex());
        double prevSavingsValue = prevSavingsCell.getNumericCellValue();

        if (prevSavingsValue != 0) {
          //Set previous savings value
          result.setPreviousSavings(
              BigDecimal.valueOf(prevSavingsValue)
                  .setScale(SCALE, RoundingMode.HALF_UP));

          //Calculate min year of incomes and expenses and set to previousSavingsDate
          List<LocalDate> dates = result.getIncomes()
              .stream()
              .map(Income::getDate)
              .collect(Collectors.toList());
          dates.addAll(
              result.getExpenses()
                  .stream()
                  .map(Expense::getDate)
                  .toList()
          );
          Optional<LocalDate> minDate = dates.stream().min(LocalDate::compareTo);
          if (minDate.isPresent()) {
            result.setPreviousSavingsDate(
                LocalDate.of(minDate.get().get(ChronoField.YEAR), 1, 1));
          } else {
            result.setPreviousSavingsDate(LocalDate.of(1970, 1, 1));
          }
        }
        return;
      }
    }
  }

  private List<OperationDraft> buildOperationDrafts(double rawValue,
                                                    String cellComment) {
    var value = BigDecimal.valueOf(rawValue).setScale(SCALE, RoundingMode.HALF_UP);

    if (StringUtils.isNotBlank(cellComment)) {
      var valuesFromComment = getValuesFromComment(cellComment);

      //Make sure the sum of the values from the comment is equal to the cell value
      if (CollectionUtils.isNotEmpty(valuesFromComment)
          && value.compareTo(
          valuesFromComment
              .stream()
              .map(OperationDraft::value)
              .reduce(BigDecimal.ZERO, BigDecimal::add)) == 0) {
        return valuesFromComment
            .stream()
            .map(cu -> new OperationDraft(cu.comment(), cu.value()))
            .toList();
      }
    }
    return List.of(new OperationDraft(cellComment, value));
  }

  private List<OperationDraft> getValuesFromComment(String cellComment) {
    final Matcher matcher = MULTI_OPERATIONS_COMMENT_PATTERN.matcher(cellComment.trim());
    final int textGroup = 1;
    final int valueGroup = 2;

    try {
      List<OperationDraft> values = new ArrayList<>();
      while (matcher.find()) {
        var text = matcher.group(textGroup).trim();
        values.add(
            new OperationDraft(
                text.isEmpty() ? null : text,
                new BigDecimal(matcher.group(valueGroup)).setScale(SCALE, RoundingMode.HALF_UP)));
      }
      return values;
    } catch (Exception e) {
      log.error(
          String.format("# XlsxParsingService: failed to parse comment of cell '%s'", cellComment),
          e);
      return List.of();
    }
  }

  private record OperationDraft(String comment, BigDecimal value) {
  }
}
