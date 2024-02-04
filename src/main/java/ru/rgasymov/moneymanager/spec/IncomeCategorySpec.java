package ru.rgasymov.moneymanager.spec;

import org.springframework.data.jpa.domain.Specification;
import ru.rgasymov.moneymanager.domain.entity.IncomeCategory;
import ru.rgasymov.moneymanager.domain.entity.IncomeCategory_;

public final class IncomeCategorySpec {

  private IncomeCategorySpec() {
  }

  public static Specification<IncomeCategory> accountIdEq(Long id) {
    return (category, cq, cb) ->
        cb.equal(category.get(IncomeCategory_.accountId), id);
  }
}
