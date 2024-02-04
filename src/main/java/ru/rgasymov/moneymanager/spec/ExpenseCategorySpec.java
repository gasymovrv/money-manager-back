package ru.rgasymov.moneymanager.spec;

import org.springframework.data.jpa.domain.Specification;
import ru.rgasymov.moneymanager.domain.entity.ExpenseCategory;
import ru.rgasymov.moneymanager.domain.entity.ExpenseCategory_;

public final class ExpenseCategorySpec {

  private ExpenseCategorySpec() {
  }

  public static Specification<ExpenseCategory> accountIdEq(Long id) {
    return (category, cq, cb) ->
        cb.equal(category.get(ExpenseCategory_.accountId), id);
  }
}
