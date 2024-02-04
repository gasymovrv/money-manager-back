package ru.rgasymov.moneymanager.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import ru.rgasymov.moneymanager.domain.entity.Expense;
import ru.rgasymov.moneymanager.domain.entity.ExpenseCategory;
import ru.rgasymov.moneymanager.domain.entity.Income;
import ru.rgasymov.moneymanager.domain.entity.IncomeCategory;

@Data
@RequiredArgsConstructor
public class FileImportResult {
  private BigDecimal previousSavings;
  private LocalDate previousSavingsDate;
  private final List<Income> incomes;
  private final List<Expense> expenses;
  private final Set<IncomeCategory> incomeCategories;
  private final Set<ExpenseCategory> expenseCategories;

  public void add(FileImportResult result) {
    incomes.addAll(result.incomes);
    expenses.addAll(result.expenses);
    incomeCategories.addAll(result.incomeCategories);
    expenseCategories.addAll(result.expenseCategories);
  }
}
