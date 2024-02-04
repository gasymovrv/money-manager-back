package ru.rgasymov.moneymanager.service.expense;

import static ru.rgasymov.moneymanager.spec.ExpenseCategorySpec.accountIdEq;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rgasymov.moneymanager.constant.CacheNames;
import ru.rgasymov.moneymanager.domain.dto.response.OperationCategoryResponseDto;
import ru.rgasymov.moneymanager.domain.entity.Expense;
import ru.rgasymov.moneymanager.domain.entity.ExpenseCategory;
import ru.rgasymov.moneymanager.domain.entity.ExpenseCategory_;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.mapper.ExpenseCategoryMapper;
import ru.rgasymov.moneymanager.repository.ExpenseCategoryRepository;
import ru.rgasymov.moneymanager.repository.ExpenseRepository;
import ru.rgasymov.moneymanager.service.BaseOperationCategoryService;
import ru.rgasymov.moneymanager.service.UserService;

@Service
@Slf4j
public class ExpenseCategoryService
    extends BaseOperationCategoryService<Expense, ExpenseCategory> {

  private final ExpenseCategoryRepository expenseCategoryRepository;

  private final ExpenseCategoryMapper expenseCategoryMapper;

  private final CacheManager cacheManager;

  public ExpenseCategoryService(
      ExpenseCategoryRepository expenseCategoryRepository,
      ExpenseRepository expenseRepository,
      ExpenseCategoryMapper expenseCategoryMapper,
      UserService userService,
      CacheManager cacheManager) {
    super(expenseRepository, expenseCategoryRepository, expenseCategoryMapper, userService);
    this.expenseCategoryRepository = expenseCategoryRepository;
    this.expenseCategoryMapper = expenseCategoryMapper;
    this.cacheManager = cacheManager;
  }

  @Cacheable(cacheNames = CacheNames.EXPENSE_CATEGORIES)
  @Transactional(readOnly = true)
  @Override
  public List<OperationCategoryResponseDto> findAll(Long accountId) {
    var result = expenseCategoryRepository.findAll(
        accountIdEq(accountId),
        Sort.by(Sort.Order.asc(ExpenseCategory_.NAME).ignoreCase())
    );
    return expenseCategoryMapper.toDtos(result);
  }

  @Cacheable(cacheNames = CacheNames.EXPENSE_CATEGORIES)
  @Transactional(readOnly = true)
  @Override
  public List<OperationCategoryResponseDto> findAllAndSetChecked(Long accountId, List<Long> ids) {
    var result = findAll(accountId);
    if (CollectionUtils.isNotEmpty(ids)) {
      result.forEach(category -> category.setChecked(ids.contains(category.getId())));
    }
    return result;
  }

  @Override
  protected ExpenseCategory buildNewOperationCategory(User currentUser, String name) {
    return ExpenseCategory.builder()
        .accountId(currentUser.getCurrentAccount().getId())
        .name(name)
        .build();
  }

  @Override
  public void clearCachedCategories() {
    Optional.ofNullable(cacheManager.getCache(CacheNames.EXPENSE_CATEGORIES)).ifPresent(
        Cache::clear);
  }
}
