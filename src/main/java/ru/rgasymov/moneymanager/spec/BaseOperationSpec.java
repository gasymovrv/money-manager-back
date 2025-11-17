package ru.rgasymov.moneymanager.spec;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import ru.rgasymov.moneymanager.domain.entity.BaseOperation;
import ru.rgasymov.moneymanager.domain.entity.BaseOperation_;

public final class BaseOperationSpec {

  private BaseOperationSpec() {
  }

  public static <R extends BaseOperation> Specification<R> savingIdIn(List<Long> savingIds) {
    return (operationRoot, cq, cb) -> operationRoot
        .get(BaseOperation_.savingId)
        .in(savingIds);
  }

  public static <R extends BaseOperation> Specification<R> accountIdEq(Long accountId) {
    return (operationRoot, cq, cb) ->
        cb.equal(operationRoot.get(BaseOperation_.accountId), accountId);
  }

  public static <R extends BaseOperation> Specification<R> dateGreaterThanOrEq(LocalDate date) {
    return (operationRoot, cq, cb) ->
        cb.greaterThanOrEqualTo(operationRoot.get(BaseOperation_.date), date);
  }

  public static <R extends BaseOperation> Specification<R> dateLessThanOrEq(LocalDate date) {
    return (operationRoot, cq, cb) ->
        cb.lessThanOrEqualTo(operationRoot.get(BaseOperation_.date), date);
  }
}
