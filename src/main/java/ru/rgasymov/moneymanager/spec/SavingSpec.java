package ru.rgasymov.moneymanager.spec;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import ru.rgasymov.moneymanager.domain.entity.Expense;
import ru.rgasymov.moneymanager.domain.entity.ExpenseCategory;
import ru.rgasymov.moneymanager.domain.entity.ExpenseCategory_;
import ru.rgasymov.moneymanager.domain.entity.Expense_;
import ru.rgasymov.moneymanager.domain.entity.Income;
import ru.rgasymov.moneymanager.domain.entity.IncomeCategory;
import ru.rgasymov.moneymanager.domain.entity.IncomeCategory_;
import ru.rgasymov.moneymanager.domain.entity.Income_;
import ru.rgasymov.moneymanager.domain.entity.Saving;
import ru.rgasymov.moneymanager.domain.entity.Saving_;
import ru.rgasymov.moneymanager.util.SpecUtils;

public final class SavingSpec {

  private SavingSpec() {
  }

  public static Specification<Saving> filterByDate(LocalDate from, LocalDate to) {
    if (from != null && to != null) {
      return dateBetween(from, to);
    } else if (from != null) {
      return dateAfter(from);
    } else if (to != null) {
      return dateBefore(to);
    }
    throw new IllegalArgumentException("FROM or TO dates must not be null");
  }

  public static Specification<Saving> dateBetween(LocalDate from, LocalDate to) {
    return (saving, cq, cb) -> cb.between(
        saving.get(Saving_.date).as(LocalDate.class), from, to);
  }

  public static Specification<Saving> dateAfter(LocalDate from) {
    return (saving, cq, cb) -> cb
        .greaterThanOrEqualTo(saving.get(Saving_.date), from);
  }

  public static Specification<Saving> dateBefore(LocalDate to) {
    return (saving, cq, cb) -> cb.lessThanOrEqualTo(saving.get(Saving_.date), to);
  }

  public static Specification<Saving> accountIdEq(Long id) {
    return (saving, cq, cb) ->
        cb.equal(saving.get(Saving_.accountId), id);
  }

  public static Specification<Saving> filterBySearchTextAndCategoryIds(
      @NotNull List<Long> incCategoryIds,
      @NotNull List<Long> expCategoryIds,
      String searchText) {

    return (saving, cq, cb) -> {
      cq.distinct(true);
      var incomeListJoin = joinIncomes(saving);
      var expenseListJoin = joinExpenses(saving);
      var incomeCategoryJoin = joinIncomeCategory(incomeListJoin);
      var expenseCategoryJoin = joinExpenseCategory(expenseListJoin);

      var incomeCategoryIdPath = incomeCategoryJoin.get(IncomeCategory_.id);
      var expenseCategoryIdPath = expenseCategoryJoin.get(ExpenseCategory_.id);

      if (StringUtils.isNotBlank(searchText)) {
        var pattern = SpecUtils.prepareSearchPattern(searchText);
        var incomeDescriptionPath = incomeListJoin.get(Income_.description);
        var expenseDescriptionPath = expenseListJoin.get(Expense_.description);
        var incomeCategoryNamePath = incomeCategoryJoin.get(IncomeCategory_.name);
        var expenseCategoryNamePath = expenseCategoryJoin.get(ExpenseCategory_.name);

        return cb.or(
            cb.and(
                incomeCategoryIdPath.in(incCategoryIds),
                cb.or(
                    cb.like(cb.lower(incomeDescriptionPath), pattern),
                    cb.like(cb.lower(incomeCategoryNamePath), pattern))),
            cb.and(
                expenseCategoryIdPath.in(expCategoryIds),
                cb.or(
                    cb.like(cb.lower(expenseDescriptionPath), pattern),
                    cb.like(cb.lower(expenseCategoryNamePath), pattern))),
            cb.and(incomeListJoin.isNull(), expenseListJoin.isNull())
        );
      } else {
        return cb.or(
            incomeCategoryIdPath.in(incCategoryIds),
            expenseCategoryIdPath.in(expCategoryIds),
            cb.and(incomeListJoin.isNull(), expenseListJoin.isNull())
        );
      }
    };
  }

  private static ListJoin<Saving, Income> joinIncomes(Root<Saving> saving) {
    return saving.join(Saving_.incomes, JoinType.LEFT);
  }

  private static ListJoin<Saving, Expense> joinExpenses(Root<Saving> saving) {
    return saving.join(Saving_.expenses, JoinType.LEFT);
  }

  private static Join<Income, IncomeCategory> joinIncomeCategory(
      ListJoin<Saving, Income> incomeListJoin) {
    return incomeListJoin.join(Income_.category, JoinType.LEFT);
  }

  private static Join<Expense, ExpenseCategory> joinExpenseCategory(
      ListJoin<Saving, Expense> expenseListJoin) {
    return expenseListJoin.join(Expense_.category, JoinType.LEFT);
  }
}
