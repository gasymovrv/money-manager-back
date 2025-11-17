package ru.rgasymov.moneymanager.spec;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import ru.rgasymov.moneymanager.domain.entity.Expense;
import ru.rgasymov.moneymanager.domain.entity.ExpenseCategory_;
import ru.rgasymov.moneymanager.domain.entity.Expense_;
import ru.rgasymov.moneymanager.util.SpecUtils;

public final class ExpenseSpec {

  private ExpenseSpec() {
  }

  public static Specification<Expense> matchBySearchText(String searchText) {
    var pattern = SpecUtils.prepareSearchPattern(searchText);
    return (expense, cq, cb) -> {
      var categoryNamePath = expense.get(Expense_.category).get(ExpenseCategory_.name);
      var descriptionPath = expense.get(Expense_.description);
      return cb.or(
          cb.like(cb.lower(categoryNamePath), pattern),
          cb.like(cb.lower(descriptionPath), pattern)
      );
    };
  }

  public static Specification<Expense> categoryIdIn(List<Long> ids) {
    return (expense, cq, cb) ->
        expense.get(Expense_.category).get(ExpenseCategory_.id).in(ids);
  }

  public static Specification<Expense> accountIdEq(Long accountId) {
    return BaseOperationSpec.accountIdEq(accountId);
  }

  public static Specification<Expense> dateGreaterThanOrEq(LocalDate date) {
    return BaseOperationSpec.dateGreaterThanOrEq(date);
  }

  public static Specification<Expense> dateLessThanOrEq(LocalDate date) {
    return BaseOperationSpec.dateLessThanOrEq(date);
  }
}
