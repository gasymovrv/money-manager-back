package ru.rgasymov.moneymanager.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rgasymov.moneymanager.domain.FileImportResult;
import ru.rgasymov.moneymanager.domain.entity.Account;
import ru.rgasymov.moneymanager.domain.entity.BaseOperation;
import ru.rgasymov.moneymanager.domain.entity.BaseOperationCategory;
import ru.rgasymov.moneymanager.domain.entity.Expense;
import ru.rgasymov.moneymanager.domain.entity.ExpenseCategory;
import ru.rgasymov.moneymanager.domain.entity.Income;
import ru.rgasymov.moneymanager.domain.entity.IncomeCategory;
import ru.rgasymov.moneymanager.domain.entity.Saving;
import ru.rgasymov.moneymanager.repository.ExpenseCategoryRepository;
import ru.rgasymov.moneymanager.repository.ExpenseRepository;
import ru.rgasymov.moneymanager.repository.IncomeCategoryRepository;
import ru.rgasymov.moneymanager.repository.IncomeRepository;
import ru.rgasymov.moneymanager.repository.SavingRepository;
import ru.rgasymov.moneymanager.spec.ExpenseCategorySpec;
import ru.rgasymov.moneymanager.spec.IncomeCategorySpec;
import ru.rgasymov.moneymanager.spec.SavingSpec;

@Service
@RequiredArgsConstructor
public class ImportService {

  private final AccountService accountService;
  private final SavingRepository savingRepository;
  private final IncomeRepository incomeRepository;
  private final ExpenseRepository expenseRepository;
  private final IncomeCategoryRepository incomeCategoryRepository;
  private final ExpenseCategoryRepository expenseCategoryRepository;

  private final UserService userService;

  @Transactional
  public void importFromFile(FileImportResult parsingResult) {
    if (accountService.isCurrentAccountEmpty()) {
      importToNewAccount(parsingResult);
      return;
    }
    importToExistentAccount(parsingResult);
  }

  private void importToNewAccount(FileImportResult parsingResult) {
    final var currentAccount = userService.getCurrentUser().getCurrentAccount();
    final var savingsMap = new HashMap<LocalDate, Saving>();
    final var previousSavings = parsingResult.getPreviousSavings();
    final var previousSavingsDate = parsingResult.getPreviousSavingsDate();

    if (previousSavings != null && previousSavingsDate != null) {
      savingsMap.put(
          previousSavingsDate,
          Saving.builder()
              .date(previousSavingsDate)
              .value(previousSavings)
              .accountId(currentAccount.getId())
              .build());
    }

    final var incomes =
        handleOperationsAndSavings(currentAccount, savingsMap, parsingResult.getIncomes(),
            BigDecimal::add);
    final var expenses =
        handleOperationsAndSavings(currentAccount, savingsMap, parsingResult.getExpenses(),
            BigDecimal::subtract);

    final var savedSavings = savingRepository.saveAll(savingsMap.values());
    final var savedIncCategories =
        incomeCategoryRepository.saveAll(parsingResult.getIncomeCategories());
    final var savedExpCategories =
        expenseCategoryRepository.saveAll(parsingResult.getExpenseCategories());

    saveOperations(
        incomes, expenses, savedSavings, savedIncCategories, savedExpCategories);
  }

  private void importToExistentAccount(FileImportResult parsingResult) {
    final var currentAccount = userService.getCurrentUser().getCurrentAccount();
    final var currentAccountId = currentAccount.getId();

    final var savingsMap = savingRepository.findAll(SavingSpec.accountIdEq(currentAccountId))
        .stream()
        .collect(Collectors.toMap(Saving::getDate, Function.identity()));

    final var incomes =
        handleOperationsAndSavings(currentAccount, savingsMap, parsingResult.getIncomes(),
            BigDecimal::add);
    final var expenses =
        handleOperationsAndSavings(currentAccount, savingsMap, parsingResult.getExpenses(),
            BigDecimal::subtract);

    final var savedSavings = savingRepository.saveAll(savingsMap.values());
    final var foundIncCategories =
        incomeCategoryRepository.findAll(IncomeCategorySpec.accountIdEq(currentAccountId));
    final var foundExpCategories =
        expenseCategoryRepository.findAll(ExpenseCategorySpec.accountIdEq(currentAccountId));

    //Save new categories and then merge them with existing ones
    final var savedIncCategories = incomeCategoryRepository.saveAll(
        getNewCategories(parsingResult.getIncomeCategories(), foundIncCategories));
    savedIncCategories.addAll(foundIncCategories);
    final var savedExpCategories = expenseCategoryRepository.saveAll(
        getNewCategories(parsingResult.getExpenseCategories(), foundExpCategories));
    savedExpCategories.addAll(foundExpCategories);

    saveOperations(
        incomes, expenses, savedSavings, savedIncCategories, savedExpCategories);
  }

  private void saveOperations(List<Income> incomes,
                              List<Expense> expenses,
                              List<Saving> savedSavings,
                              List<IncomeCategory> savedIncCategories,
                              List<ExpenseCategory> savedExpCategories) {

    final var incCategoriesMap = convertCategoriesToMap(savedIncCategories);
    final var expCategoriesMap = convertCategoriesToMap(savedExpCategories);
    final var savingsMap = savedSavings
        .stream()
        .collect(Collectors.toMap(Saving::getDate, Function.identity()));

    incomes.forEach(income -> {
      var categoryName = income.getCategory().getName();
      var date = income.getDate();
      income.setCategory(incCategoriesMap.get(categoryName));
      income.setSavingId(savingsMap.get(date).getId());
    });
    expenses.forEach(expense -> {
      var categoryName = expense.getCategory().getName();
      var date = expense.getDate();
      expense.setCategory(expCategoriesMap.get(categoryName));
      expense.setSavingId(savingsMap.get(date).getId());
    });
    incomeRepository.saveAll(incomes);
    expenseRepository.saveAll(expenses);
  }

  private <T extends BaseOperation> List<T> handleOperationsAndSavings(
      final Account currentAccount,
      final Map<LocalDate, Saving> savingsMap,
      final List<T> operations,
      final BiFunction<BigDecimal, BigDecimal, BigDecimal> setValueFunc) {

    final var operationsCopy = new ArrayList<>(operations);
    operationsCopy.sort(Comparator.comparing(BaseOperation::getDate));
    operationsCopy.forEach(income -> {
      final var date = income.getDate();
      final var value = income.getValue();
      recalculateMap(savingsMap, date, value, setValueFunc, currentAccount);
    });
    return operationsCopy;
  }

  private <T extends BaseOperationCategory> List<T> getNewCategories(Set<T> categoriesFromFile,
                                                                     List<T> foundCategories) {
    final var foundCategoryNames =
        foundCategories
            .stream()
            .map(BaseOperationCategory::getName)
            .collect(Collectors.toSet());
    return categoriesFromFile
        .stream()
        .filter(category -> !foundCategoryNames.contains(category.getName()))
        .toList();
  }

  private <T extends BaseOperationCategory> Map<String, T> convertCategoriesToMap(
      List<T> categories) {
    return categories
        .stream()
        .collect(Collectors.toMap(BaseOperationCategory::getName, Function.identity()));
  }

  private void recalculateMap(Map<LocalDate, Saving> savings,
                              LocalDate date,
                              BigDecimal value,
                              BiFunction<BigDecimal, BigDecimal, BigDecimal> setValueFunc,
                              Account currentAccount) {
    final var saving = savings.get(date);

    if (saving != null) {
      saving.setValue(setValueFunc.apply(saving.getValue(), value));
    } else {
      var lastSavingValue = savings.values()
          .stream()
          .filter(s -> s.getDate().isBefore(date))
          .max(Comparator.comparing(Saving::getDate))
          .map(Saving::getValue)
          .orElse(BigDecimal.ZERO);

      var newSaving = Saving.builder()
          .date(date)
          .value(setValueFunc.apply(lastSavingValue, value))
          .accountId(currentAccount.getId())
          .build();
      savings.put(date, newSaving);
    }
    savings.values()
        .stream()
        .filter(s -> s.getDate().isAfter(date))
        .forEach(s -> s.setValue(setValueFunc.apply(s.getValue(), value)));
  }
}
