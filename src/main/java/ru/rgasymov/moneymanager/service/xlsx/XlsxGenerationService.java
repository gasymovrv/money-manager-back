package ru.rgasymov.moneymanager.service.xlsx;

import static ru.rgasymov.moneymanager.util.DateUtil.getDatesBetweenInclusive;
import static ru.rgasymov.moneymanager.util.DateUtil.getFirstDateOfMonth;
import static ru.rgasymov.moneymanager.util.DateUtil.getLastDateOfMonth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import ru.rgasymov.moneymanager.domain.FileExportData;
import ru.rgasymov.moneymanager.domain.dto.response.OperationCategoryResponseDto;
import ru.rgasymov.moneymanager.domain.dto.response.OperationResponseDto;
import ru.rgasymov.moneymanager.domain.dto.response.SavingResponseDto;

@Service
@Slf4j
public class XlsxGenerationService {

  private static final int TEMPLATE_SHEET_INDEX = 0;
  private static final int FIRST_ROW = 0;
  /**
   * Indexes of columns with style samples.
   */
  private static final int DATE_COL = 0;
  private static final int START_INC_COL = 1;
  private static final int START_INC_SUM_COL = 2;
  private static final int START_EXP_COL = 3;
  private static final int START_EXP_SUM_COL = 4;
  private static final int START_SAVING_COL = 5;
  /**
   * Index of the row with category names.
   */
  private static final int CATEGORIES_ROW = 1;
  /**
   * Index of sample rows for income and expense.
   */
  private static final int START_DATA_ROW_SAMPLE = 3;
  private static final int START_DATA_ROW_BORDER_SAMPLE = 4;
  /**
   * Index of the first income column.
   */
  private static final int START_DATA_COLUMN = 1;
  /**
   * Column names.
   */
  private static final String INCOMES_COLUMN_NAME = "Incomes";
  private static final String INCOMES_SUM_COLUMN_NAME = "Incomes sum";
  private static final String EXPENSES_COLUMN_NAME = "Expenses";
  private static final String EXPENSES_SUM_COLUMN_NAME = "Expenses sum";
  private static final String SAVINGS_COLUMN_NAME = "Savings";
  private static final String PREV_SAVINGS_COLUMN_NAME = "Previous savings";
  /**
   * Index of the previous savings row.
   */
  private static final int PREVIOUS_SAVINGS_ROW = 2;
  private static final String COLLAPSED_COMMENT = "%s (%s); ";
  private static final String COLLAPSED_COMMENT_SIMPLE = "%s; ";
  @Value("${xlsx.show-empty-rows}")
  private boolean showEmptyRows;

  public Resource generate(Resource xlsxTemplateFile,
                           FileExportData data) throws IOException {
    try (var wb = new XSSFWorkbook(xlsxTemplateFile.getInputStream());
         var os = new ByteArrayOutputStream()) {
      //------- Create sheets -------
      var savingsMap = data.savings()
          .stream()
          .collect(Collectors.groupingBy(item -> item.getDate().get(ChronoField.YEAR)));

      var lastYearSaving = BigDecimal.ZERO;
      for (var entry : savingsMap.entrySet()) {
        final var year = entry.getKey();
        final var savingsOfYear = entry.getValue();
        final var sheet = wb.cloneSheet(TEMPLATE_SHEET_INDEX, year.toString());

        fillSheet(
            sheet,
            data.incomeCategories(),
            data.expenseCategories(),
            savingsOfYear,
            lastYearSaving);

        //Remove sample row if it's visible
        if (savingsOfYear.size() < 2 && !showEmptyRows) {
          sheet.removeRow(sheet.getRow(START_DATA_ROW_BORDER_SAMPLE));
        }

        lastYearSaving =
            savingsOfYear.stream()
                .max(Comparator.comparing(SavingResponseDto::getDate))
                .map(SavingResponseDto::getValue)
                .orElse(BigDecimal.ZERO);
      }
      wb.removeSheetAt(TEMPLATE_SHEET_INDEX);

      wb.write(os);
      return new ByteArrayResource(os.toByteArray());
    }
  }

  private void fillSheet(XSSFSheet sheet,
                         List<OperationCategoryResponseDto> incomeCategories,
                         List<OperationCategoryResponseDto> expenseCategories,
                         List<SavingResponseDto> savings,
                         BigDecimal lastYearSaving) {
    final XSSFRow firstRow = sheet.getRow(FIRST_ROW);
    final XSSFRow categoriesRow = sheet.getRow(CATEGORIES_ROW);
    final CellStyle headerStyle = firstRow.getCell(DATE_COL).getCellStyle();

    //------------------------------ Create head rows --------------------------------------------
    //------- Create head columns for Incomes -----------
    final var incColumnMap = new HashMap<String, Integer>();
    final int incCategoryLastCol = createCategoriesHeader(
        START_DATA_COLUMN,
        incomeCategories
            .stream()
            .map(OperationCategoryResponseDto::getName)
            .sorted()
            .collect(Collectors.toList()),
        firstRow,
        categoriesRow,
        headerStyle,
        incColumnMap,
        INCOMES_COLUMN_NAME,
        INCOMES_SUM_COLUMN_NAME
    );

    //------- Create head columns for Expenses -----------
    final var expColumnMap = new HashMap<String, Integer>();
    final int expCategoryLastCol = createCategoriesHeader(
        incCategoryLastCol + 1,
        expenseCategories
            .stream()
            .map(OperationCategoryResponseDto::getName)
            .sorted()
            .collect(Collectors.toList()),
        firstRow,
        categoriesRow,
        headerStyle,
        expColumnMap,
        EXPENSES_COLUMN_NAME,
        EXPENSES_SUM_COLUMN_NAME
    );

    //------- Create head column for Savings -----------
    final int savingsCol = expCategoryLastCol + 1;
    final var headCell = firstRow.createCell(savingsCol, CellType.STRING);
    headCell.setCellValue(SAVINGS_COLUMN_NAME);
    headCell.setCellStyle(headerStyle);

    //------- Merge head columns -------
    if (START_DATA_COLUMN != incCategoryLastCol) {
      sheet.addMergedRegion(
          new CellRangeAddress(FIRST_ROW, FIRST_ROW, START_DATA_COLUMN, incCategoryLastCol));
    }

    final var expCategoryFirstCol = incCategoryLastCol + 1;
    if (expCategoryFirstCol != expCategoryLastCol) {
      sheet.addMergedRegion(
          new CellRangeAddress(FIRST_ROW, FIRST_ROW, expCategoryFirstCol, expCategoryLastCol));
    }

    sheet.addMergedRegion(
        new CellRangeAddress(FIRST_ROW, CATEGORIES_ROW, savingsCol, savingsCol));

    //------------------------------ Create data rows --------------------------------------------
    createDataRows(
        sheet,
        savings,
        headerStyle,
        incCategoryLastCol,
        expCategoryLastCol,
        savingsCol,
        incColumnMap,
        expColumnMap,
        lastYearSaving
    );
  }

  private void createDataRows(XSSFSheet sheet,
                              List<SavingResponseDto> savings,
                              CellStyle headerStyle,
                              int incCategoryLastCol,
                              int expCategoryLastCol,
                              int savingsCol,
                              HashMap<String, Integer> incColumnMap,
                              HashMap<String, Integer> expColumnMap,
                              BigDecimal lastYearSaving) {
    //------- Get rows styles -------
    final var styles = getStyles(sheet.getWorkbook(), START_DATA_ROW_SAMPLE);

    //------- Fill previous savings row -------
    final var prevSavingRow = sheet.createRow(PREVIOUS_SAVINGS_ROW);
    createStyledCells(
        prevSavingRow,
        savingsCol,
        incCategoryLastCol,
        expCategoryLastCol,
        styles,
        incColumnMap,
        expColumnMap
    );
    var cell = prevSavingRow.createCell(DATE_COL, CellType.STRING);
    cell.setCellValue(PREV_SAVINGS_COLUMN_NAME);
    cell.setCellStyle(headerStyle);

    final var previousSavings = savings.stream()
        .filter(this::isPreviousSaving)
        .map(SavingResponseDto::getValue)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    cell = prevSavingRow.getCell(savingsCol);
    cell.setCellValue(previousSavings.doubleValue());

    //------- Create incomes and expenses rows -------
    final var savingRows = handleSavings(
        savings, previousSavings, lastYearSaving);

    var startRow = START_DATA_ROW_SAMPLE;
    for (int i = 0; i < savingRows.size(); i++) {
      final var savingRow = savingRows.get(i);
      final var savingRowDate = savingRow.getDate();
      final var row = sheet.createRow(startRow++);

      //Define bottom borders of months
      var isEndOfMonth = isEndOfMonth(savingRows, i, savingRowDate);

      //Create styled cells
      createStyledCells(
          row,
          savingsCol,
          incCategoryLastCol,
          expCategoryLastCol,
          isEndOfMonth
              ? getStyles(sheet.getWorkbook(), START_DATA_ROW_BORDER_SAMPLE)
              : styles,
          incColumnMap,
          expColumnMap
      );

      //Fill date cell
      var dataCell = row.getCell(DATE_COL);
      dataCell.setCellValue(savingRowDate);

      //Fill income cells
      fillOperationCells(sheet, row, incColumnMap, savingRow.getIncomesByCategory());

      //Fill incomes sum cell
      dataCell = row.getCell(incCategoryLastCol);
      dataCell.setCellValue(savingRow.getIncomesSum().doubleValue());

      //Fill expense cells
      fillOperationCells(sheet, row, expColumnMap, savingRow.getExpensesByCategory());

      //Fill expenses sum cell
      dataCell = row.getCell(expCategoryLastCol);
      dataCell.setCellValue(savingRow.getExpensesSum().doubleValue());

      //Fill saving cell
      dataCell = row.getCell(savingsCol);
      dataCell.setCellValue(savingRow.getValue().doubleValue());

      //Mark current date
      if (savingRowDate.equals(LocalDate.now())) {
        dataCell = row.createCell(savingsCol + 1, CellType.STRING);
        dataCell.setCellValue("Download date");

        final var style = sheet.getWorkbook().createCellStyle();
        style.setFillForegroundColor(IndexedColors.RED.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        dataCell.setCellStyle(style);
      }
    }
  }

  private void fillOperationCells(XSSFSheet sheet,
                                  XSSFRow row,
                                  HashMap<String, Integer> operationsColumnMap,
                                  Map<String, List<OperationResponseDto>> operationsByCategory) {
    //Get incomes by categories
    final var operationsFlatMap = new HashMap<String, OperationResponseDto>();
    operationsByCategory
        .values()
        .stream()
        .flatMap(Collection::stream)
        .forEach((inc) ->
            operationsFlatMap.merge(inc.getCategory().getName(), inc, this::mergeOperations));

    //Fill income cells
    operationsFlatMap.values()
        .forEach(inc -> fillOperationCell(sheet, row, operationsColumnMap, inc));
  }

  private void fillOperationCell(XSSFSheet sheet,
                                 XSSFRow row,
                                 HashMap<String, Integer> operationColumnMap,
                                 OperationResponseDto operation) {
    final var colNumByCategory = operationColumnMap.get(operation.getCategory().getName());
    final var cell = row.getCell(colNumByCategory);
    cell.setCellValue(operation.getValue().doubleValue());

    final var description = operation.getDescription();
    if (StringUtils.isNoneBlank(description)) {
      addComment(sheet, cell, description);
    }
  }

  private boolean isPreviousSaving(SavingResponseDto saving) {
    return MapUtils.isEmpty(saving.getIncomesByCategory())
        && MapUtils.isEmpty(saving.getExpensesByCategory());
  }

  public void addComment(Sheet sheet,
                         Cell cell,
                         String commentText) {
    final var factory = sheet.getWorkbook().getCreationHelper();
    final var anchor = factory.createClientAnchor();
    //Show comment box in bottom right corner
    anchor
        .setCol1(cell.getColumnIndex() + 1); //the box of the comment starts at this given column...
    anchor.setCol2(cell.getColumnIndex() + 3); //...and ends at that given column
    anchor.setRow1(cell.getRowIndex() + 1); //one row below the cell...
    anchor.setRow2(cell.getRowIndex() + 5); //...and 4 rows high

    final var drawing = sheet.createDrawingPatriarch();
    final var comment = drawing.createCellComment(anchor);
    //set the comment text and author
    comment.setString(factory.createRichTextString(commentText));

    cell.setCellComment(comment);
  }

  private int createCategoriesHeader(int startCol,
                                     List<String> categories,
                                     XSSFRow firstRow,
                                     XSSFRow categoriesRow,
                                     CellStyle headerStyle,
                                     HashMap<String, Integer> columnMap,
                                     String headerColName,
                                     String headerSumColName) {
    var headCell = firstRow.createCell(startCol, CellType.STRING);
    headCell.setCellValue(headerColName);
    headCell.setCellStyle(headerStyle);
    for (String categoryName : categories) {
      headCell = categoriesRow.createCell(startCol, CellType.STRING);
      headCell.setCellValue(categoryName);
      headCell.setCellStyle(headerStyle);
      columnMap.put(categoryName, startCol);
      startCol++;
    }
    headCell = categoriesRow.createCell(startCol, CellType.STRING);
    headCell.setCellValue(headerSumColName);
    headCell.setCellStyle(headerStyle);
    return startCol;
  }

  private void createStyledCells(XSSFRow row,
                                 int savingsCol,
                                 int incCategoryCol,
                                 int expCategoryCol,
                                 Styles styles,
                                 HashMap<String, Integer> incColumnMap,
                                 HashMap<String, Integer> expColumnMap) {
    final CellStyle dateStyle = styles.dateStyle();
    final CellStyle savingsStyle = styles.savingsStyle();
    final CellStyle incSumStyle = styles.incSumStyle();
    final CellStyle expSumStyle = styles.expSumStyle();
    final CellStyle incStyle = styles.incStyle();
    final CellStyle expStyle = styles.expStyle();
    int startColNum = DATE_COL;

    //Create date cell
    var cell = row.createCell(startColNum, CellType.NUMERIC);
    cell.setCellStyle(dateStyle);

    //Create incomes and expenses cells
    while (startColNum < savingsCol) {
      if (incColumnMap.containsValue(startColNum)) {
        cell = row.createCell(startColNum, CellType.NUMERIC);
        cell.setCellStyle(incStyle);
      }
      if (expColumnMap.containsValue(startColNum)) {
        cell = row.createCell(startColNum, CellType.NUMERIC);
        cell.setCellStyle(expStyle);
      }
      startColNum++;
    }

    //Create incomes sum cell
    cell = row.createCell(incCategoryCol, CellType.NUMERIC);
    cell.setCellStyle(incSumStyle);

    //Create expenses sum cell
    cell = row.createCell(expCategoryCol, CellType.NUMERIC);
    cell.setCellStyle(expSumStyle);

    //Create saving cell
    cell = row.createCell(savingsCol, CellType.NUMERIC);
    cell.setCellStyle(savingsStyle);
  }


  private void collapseComments(OperationResponseDto dto1, OperationResponseDto dto2) {
    String description = "";
    if (StringUtils.isNotBlank(dto1.getDescription())
        && !dto1.isDescriptionCollapsed()) {
      description = String.format(COLLAPSED_COMMENT,
          dto1.getDescription(),
          dto1.getValue());
    } else if (!dto1.isDescriptionCollapsed()) {
      description = String.format(COLLAPSED_COMMENT_SIMPLE, dto1.getValue());
    }
    if (!dto1.isDescriptionCollapsed()) {
      dto1.setDescriptionCollapsed(true);
      dto1.setDescription(null);
    }

    if (StringUtils.isNotBlank(dto2.getDescription())) {
      description += String.format(COLLAPSED_COMMENT,
          dto2.getDescription(),
          dto2.getValue());
    } else {
      description += String.format(COLLAPSED_COMMENT_SIMPLE, dto2.getValue());
    }

    String resultDescr = null;
    if (StringUtils.isNotBlank(dto1.getDescription())
        || StringUtils.isNotBlank(description)) {
      if (dto1.getDescription() == null) {
        resultDescr = description;
      } else {
        resultDescr = dto1.getDescription() + description;
      }
    }
    dto1.setDescription(resultDescr);
  }

  private OperationResponseDto mergeOperations(OperationResponseDto dto1,
                                               OperationResponseDto dto2) {
    collapseComments(dto1, dto2);
    dto1.setValue(dto1.getValue().add(dto2.getValue()));
    return dto1;
  }

  private List<SavingResponseDto> handleSavings(final List<SavingResponseDto> savings,
                                                final BigDecimal previousSavings,
                                                final BigDecimal lastYearSaving) {
    //Remove previous saving value to resolve possible conflicts between empty rows
    var savingRows = savings.stream()
        .filter(dto -> !isPreviousSaving(dto))
        .toList();
    if (!showEmptyRows) {
      return savingRows;
    }

    //------- Add empty rows to fill every day in month -------
    var savingRowsByDate = savingRows
        .stream()
        .collect(Collectors.groupingBy(SavingResponseDto::getDate));

    final var firstDate =
        savingRows.stream().map(SavingResponseDto::getDate)
            .min(LocalDate::compareTo)
            .orElse(LocalDate.now());

    final var lastDate =
        savingRows.stream().map(SavingResponseDto::getDate)
            .max(LocalDate::compareTo)
            .orElse(LocalDate.now());

    var allDates = getDatesBetweenInclusive(
        getFirstDateOfMonth(firstDate),
        getLastDateOfMonth(lastDate));

    //It's for filling the 'Savings' column in empty rows
    var lastSavingValue = previousSavings.compareTo(BigDecimal.ZERO) != 0
        ? previousSavings
        : lastYearSaving;

    var result = new ArrayList<SavingResponseDto>();

    for (var date : allDates) {
      final var savingsPerDay = savingRowsByDate.get(date);

      if (savingsPerDay == null) {
        //Empty row
        result.add(
            SavingResponseDto.builder()
                .date(date)
                .value(lastSavingValue)
                .incomesByCategory(Map.of())
                .expensesByCategory(Map.of())
                .build());
      } else {
        lastSavingValue = savingsPerDay
            .stream()
            .findFirst()
            .map(SavingResponseDto::getValue)
            .orElse(lastSavingValue);
        //Filled row (savings in list have only one date and will be merged)
        result.addAll(savingsPerDay);
      }
    }
    return result;
  }

  private Styles getStyles(XSSFWorkbook wb, int sampleRowIndex) {
    final CellStyle dateStyle = wb.getSheetAt(TEMPLATE_SHEET_INDEX)
        .getRow(sampleRowIndex).getCell(DATE_COL).getCellStyle();

    final CellStyle incStyle = wb.getSheetAt(TEMPLATE_SHEET_INDEX)
        .getRow(sampleRowIndex).getCell(START_INC_COL).getCellStyle();

    final CellStyle incSumStyle = wb.getSheetAt(TEMPLATE_SHEET_INDEX)
        .getRow(sampleRowIndex).getCell(START_INC_SUM_COL).getCellStyle();

    final CellStyle expStyle = wb.getSheetAt(TEMPLATE_SHEET_INDEX)
        .getRow(sampleRowIndex).getCell(START_EXP_COL).getCellStyle();

    final CellStyle expSumStyle = wb.getSheetAt(TEMPLATE_SHEET_INDEX)
        .getRow(sampleRowIndex).getCell(START_EXP_SUM_COL).getCellStyle();

    final CellStyle savingsStyle = wb.getSheetAt(TEMPLATE_SHEET_INDEX)
        .getRow(sampleRowIndex).getCell(START_SAVING_COL).getCellStyle();

    return new Styles(dateStyle, incStyle, incSumStyle, expStyle, expSumStyle, savingsStyle);
  }

  private boolean isEndOfMonth(List<SavingResponseDto> savingRows,
                               int currentIndex,
                               LocalDate savingRowDate) {
    if (showEmptyRows) {
      return savingRowDate.isEqual(getLastDateOfMonth(savingRowDate));
    }

    if (currentIndex < savingRows.size() - 1) {
      final var nextRow = savingRows.get(currentIndex + 1);
      return nextRow.getDate().getMonth().getValue() != savingRowDate.getMonth().getValue();
    }
    return currentIndex == savingRows.size() - 1;
  }

  private record Styles(
      CellStyle dateStyle,
      CellStyle incStyle,
      CellStyle incSumStyle,
      CellStyle expStyle,
      CellStyle expSumStyle,
      CellStyle savingsStyle
  ) {
  }
}
