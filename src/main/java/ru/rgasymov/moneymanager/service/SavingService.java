package ru.rgasymov.moneymanager.service;

import static ru.rgasymov.moneymanager.util.SpecUtils.andOptionally;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rgasymov.moneymanager.domain.dto.request.SavingCriteriaDto;
import ru.rgasymov.moneymanager.domain.dto.response.OperationCategoryResponseDto;
import ru.rgasymov.moneymanager.domain.dto.response.SavingResponseDto;
import ru.rgasymov.moneymanager.domain.dto.response.SavingSearchResultDto;
import ru.rgasymov.moneymanager.domain.entity.Expense;
import ru.rgasymov.moneymanager.domain.entity.Income;
import ru.rgasymov.moneymanager.domain.entity.Saving;
import ru.rgasymov.moneymanager.domain.enums.Period;
import ru.rgasymov.moneymanager.mapper.SavingGroupMapper;
import ru.rgasymov.moneymanager.mapper.SavingMapper;
import ru.rgasymov.moneymanager.repository.ExpenseRepository;
import ru.rgasymov.moneymanager.repository.IncomeRepository;
import ru.rgasymov.moneymanager.repository.SavingRepository;
import ru.rgasymov.moneymanager.service.expense.ExpenseCategoryService;
import ru.rgasymov.moneymanager.service.income.IncomeCategoryService;
import ru.rgasymov.moneymanager.spec.BaseOperationSpec;
import ru.rgasymov.moneymanager.spec.ExpenseSpec;
import ru.rgasymov.moneymanager.spec.IncomeSpec;
import ru.rgasymov.moneymanager.spec.SavingSpec;

@Service
@RequiredArgsConstructor
public class SavingService {

  private final SavingRepository savingRepository;
  private final IncomeRepository incomeRepository;
  private final ExpenseRepository expenseRepository;

  private final SavingMapper savingMapper;
  private final SavingGroupMapper savingGroupMapper;

  private final UserService userService;
  private final IncomeCategoryService incomeCategoryService;
  private final ExpenseCategoryService expenseCategoryService;

  /**
   * Searches for savings based on the provided criteria and returns paginated results.
   *
   * <p>The search process involves several steps:
   * <ol>
   *   <li>Retrieves the current user and their active account</li>
   *   <li>Loads income and expense categories, marking selected ones as checked</li>
   *   <li>Builds a JPA Specification from the criteria (date range, category filters, search text)</li>
   *   <li>Executes a paginated query to find matching Saving entities</li>
   *   <li><b>Explicitly filters operations</b> - replaces lazy-loaded collections with filtered results
   *       (see {@link #fillOperationsExplicitly(List, SavingCriteriaDto)})</li>
   *   <li>Maps entities to DTOs, either grouped by period or as individual days</li>
   *   <li>Returns the result with total count and category lists</li>
   * </ol>
   *
   * <p><b>Important:</b> The {@code fillOperationsExplicitly} call is crucial when filtering by categories
   * or search text. Without it, the mapper would access lazy-loaded collections containing ALL operations
   * for the found savings, not just the filtered ones, resulting in incorrect data in the response.
   *
   * @param criteria search criteria including date range, pagination, sorting, category filters, and search text
   * @return search result containing filtered savings, total count, and category lists
   */
  @Transactional(readOnly = true)
  public SavingSearchResultDto search(SavingCriteriaDto criteria) {
    var currentUser = userService.getCurrentUser();
    var currentAccountId = currentUser.getCurrentAccount().getId();

    var incCategories =
        incomeCategoryService.findAllAndSetChecked(
            currentAccountId, criteria.getIncomeCategoryIds());
    var expCategories =
        expenseCategoryService.findAllAndSetChecked(
            currentAccountId, criteria.getExpenseCategoryIds());

    Specification<Saving> criteriaAsSpec =
        applySavingCriteria(criteria, incCategories, expCategories);

    Page<Saving> page = savingRepository.findAll(criteriaAsSpec,
        PageRequest.of(
            criteria.getPageNum(),
            criteria.getPageSize(),
            Sort.by(criteria.getSortDirection(),
                criteria.getSortBy().getFieldName())));
    var savings = page.getContent();

    fillOperationsExplicitly(savings, criteria);

    List<SavingResponseDto> result;
    if (criteria.getGroupBy() != Period.DAY) {
      result = savingGroupMapper.toGroupDtos(savings, criteria.getGroupBy());
    } else {
      result = savingMapper.toDtos(savings);
    }

    return SavingSearchResultDto
        .builder()
        .result(result)
        .totalElements(page.getTotalElements())
        .incomeCategories(incCategories)
        .expenseCategories(expCategories)
        .build();
  }

  @Transactional(readOnly = true)
  public Saving findByDate(LocalDate date) {
    var currentUser = userService.getCurrentUser();
    var currentAccountId = currentUser.getCurrentAccount().getId();
    return savingRepository.findByDateAndAccountId(date, currentAccountId).orElseThrow(() ->
        new EntityNotFoundException(
            String.format("Could not find saving by date = '%s' in the database",
                date)));
  }

  @Transactional
  public void increase(BigDecimal value, LocalDate date) {
    recalculate(date, value, BigDecimal::add,
        savingRepository::increaseValueByDateGreaterThan);
  }

  @Transactional
  public void decrease(BigDecimal value, LocalDate date) {
    recalculate(date, value, BigDecimal::subtract,
        savingRepository::decreaseValueByDateGreaterThan);
  }

  @Transactional
  public void updateAfterDeletionOperation(LocalDate date) {
    var currentUser = userService.getCurrentUser();
    var currentAccountId = currentUser.getCurrentAccount().getId();

    savingRepository.findByDateAndAccountId(date, currentAccountId).ifPresent(saving -> {
      if (CollectionUtils.isEmpty(saving.getIncomes())
          && CollectionUtils.isEmpty(saving.getExpenses())) {
        savingRepository.delete(saving);
      }
    });
  }

  /**
   * It retrieves the current user and account,
   * then updates or creates a saving based on the input date and value.
   * Finally, it recalculates the value of other savings.
   *
   * @param date                  The date of the saving (can be new or existing)
   * @param value                 The value of the saving
   * @param setValueFunc          The function that sets the value of the saving
   *                              based on the input value
   * @param recalculateOthersFunc The function that recalculates the value of other savings
   */
  private void recalculate(LocalDate date,
                           BigDecimal value,
                           BiFunction<BigDecimal, BigDecimal, BigDecimal> setValueFunc,
                           RecalculateFunc recalculateOthersFunc) {
    var currentUser = userService.getCurrentUser();
    var currentAccountId = currentUser.getCurrentAccount().getId();

    //Find the saving by date and recalculate its value by the specified value
    Optional<Saving> savingOpt = savingRepository.findByDateAndAccountId(date, currentAccountId);
    if (savingOpt.isPresent()) {
      Saving saving = savingOpt.get();
      saving.setValue(setValueFunc.apply(saving.getValue(), value));
      savingRepository.save(saving);
    } else {
      savingOpt = savingRepository
          .findFirstByDateLessThanAndAccountIdOrderByDateDesc(date, currentAccountId);

      var newSaving = Saving.builder()
          .date(date)
          .accountId(currentUser.getCurrentAccount().getId())
          .build();

      if (savingOpt.isPresent()) {
        newSaving.setValue(setValueFunc.apply(savingOpt.get().getValue(), value));
      } else {
        newSaving.setValue(setValueFunc.apply(BigDecimal.ZERO, value));
      }
      savingRepository.save(newSaving);
    }

    //Recalculate the value of other savings by the specified value
    recalculateOthersFunc.recalculate(value, date, currentAccountId);
  }

  /**
   * Explicitly filters and replaces the operations (incomes/expenses) in the savings entities
   * based on the search criteria.
   *
   * <p><b>Why this method is necessary:</b>
   *
   * <p>When the main query filters Savings by category IDs or search text using JPA joins,
   * it only determines <i>which Saving entities to return</i>, but does NOT filter the contents
   * of their lazy-loaded {@code incomes} and {@code expenses} collections.
   *
   * <p>When the mapper later accesses these collections (e.g., {@code entity.getExpenses().forEach(...)}),
   * Hibernate will load ALL operations for those savings, not just the filtered ones.
   * This causes incorrect results where operations from unselected categories appear in the response.
   *
   * <p><b>Solution:</b>
   *
   * <p>This method explicitly queries the Income and Expense repositories with the same filters,
   * then replaces the lazy collections in each Saving entity with only the filtered operations.
   * This ensures that when the mapper accesses the collections, it only sees the operations
   * that match the search criteria.
   *
   * <p><b>Performance note:</b> This method is only executed when category filters or search text
   * are provided. For simple date-range queries, it returns early without additional database calls.
   *
   * @param savings  the list of Saving entities returned by the main query
   * @param criteria the search criteria containing category filters and/or search text
   */
  private void fillOperationsExplicitly(List<Saving> savings,
                                        SavingCriteriaDto criteria) {
    if (CollectionUtils.isEmpty(criteria.getIncomeCategoryIds())
        && CollectionUtils.isEmpty(criteria.getExpenseCategoryIds())
        && StringUtils.isBlank(criteria.getSearchText())) {
      return;
    }

    var currentUser = userService.getCurrentUser();
    var currentAccountId = currentUser.getCurrentAccount().getId();
    var savingIds = savings.stream().map(Saving::getId).toList();
    var incomeMap = new HashMap<Long, List<Income>>();
    var expenseMap = new HashMap<Long, List<Expense>>();

    incomeRepository.findAll(applyIncomeCriteria(currentAccountId, savingIds, criteria))
        .forEach(inc -> {
          ArrayList<Income> value = new ArrayList<>();
          value.add(inc);
          incomeMap.merge(inc.getSavingId(), value,
              (oldValue, newValue) -> {
                oldValue.addAll(newValue);
                return oldValue;
              });
        });

    expenseRepository.findAll(applyExpenseCriteria(currentAccountId, savingIds, criteria))
        .forEach(exp -> {
          ArrayList<Expense> value = new ArrayList<>();
          value.add(exp);
          expenseMap.merge(exp.getSavingId(), value,
              (oldValue, newValue) -> {
                oldValue.addAll(newValue);
                return oldValue;
              });
        });

    savings.forEach(saving -> {
      saving.setIncomes(Optional
          .ofNullable(incomeMap.get(saving.getId()))
          .orElse(List.of()));
      saving.setExpenses(Optional
          .ofNullable(expenseMap.get(saving.getId()))
          .orElse(List.of()));
    });
  }

  private Specification<Saving> applySavingCriteria(
      SavingCriteriaDto criteria,
      List<OperationCategoryResponseDto> incCategories,
      List<OperationCategoryResponseDto> expCategories) {
    var currentUser = userService.getCurrentUser();
    var currentAccountId = currentUser.getCurrentAccount().getId();

    Specification<Saving> criteriaAsSpec = SavingSpec.accountIdEq(currentAccountId);

    LocalDate from = criteria.getFrom();
    LocalDate to = criteria.getTo();
    if (from != null || to != null) {
      criteriaAsSpec = criteriaAsSpec.and(SavingSpec.filterByDate(from, to));
    }

    if (CollectionUtils.isNotEmpty(criteria.getIncomeCategoryIds())
        || CollectionUtils.isNotEmpty(criteria.getExpenseCategoryIds())
        || StringUtils.isNotBlank(criteria.getSearchText())) {

      var selectedIncCategoryIds = incCategories.stream()
          .filter(OperationCategoryResponseDto::isChecked)
          .map(OperationCategoryResponseDto::getId)
          .toList();

      var selectedExpCategoryIds = expCategories.stream()
          .filter(OperationCategoryResponseDto::isChecked)
          .map(OperationCategoryResponseDto::getId)
          .toList();

      criteriaAsSpec = criteriaAsSpec.and(SavingSpec.filterBySearchTextAndCategoryIds(
          selectedIncCategoryIds,
          selectedExpCategoryIds,
          criteria.getSearchText())
      );
    }

    return criteriaAsSpec;
  }

  private Specification<Income> applyIncomeCriteria(Long accountId,
                                                    List<Long> savingIds,
                                                    SavingCriteriaDto criteria) {
    Specification<Income> incomeSpec = IncomeSpec.accountIdEq(accountId)
        .and(BaseOperationSpec.savingIdIn(savingIds));
    incomeSpec = andOptionally(incomeSpec, IncomeSpec::matchBySearchText, criteria.getSearchText());
    return andOptionally(incomeSpec, IncomeSpec::categoryIdIn, criteria.getIncomeCategoryIds());
  }

  private Specification<Expense> applyExpenseCriteria(Long accountId,
                                                      List<Long> savingIds,
                                                      SavingCriteriaDto criteria) {
    Specification<Expense> expenseSpec = ExpenseSpec.accountIdEq(accountId)
        .and(BaseOperationSpec.savingIdIn(savingIds));
    expenseSpec = andOptionally(expenseSpec, ExpenseSpec::matchBySearchText, criteria.getSearchText());
    return andOptionally(expenseSpec, ExpenseSpec::categoryIdIn,
        criteria.getExpenseCategoryIds());
  }

  interface RecalculateFunc {
    void recalculate(BigDecimal decrement,
                     LocalDate date,
                     Long accountId);
  }
}
