package ru.rgasymov.moneymanager.service.income;

import static ru.rgasymov.moneymanager.spec.IncomeCategorySpec.accountIdEq;

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
import ru.rgasymov.moneymanager.domain.entity.Income;
import ru.rgasymov.moneymanager.domain.entity.IncomeCategory;
import ru.rgasymov.moneymanager.domain.entity.IncomeCategory_;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.mapper.IncomeCategoryMapper;
import ru.rgasymov.moneymanager.repository.IncomeCategoryRepository;
import ru.rgasymov.moneymanager.repository.IncomeRepository;
import ru.rgasymov.moneymanager.service.BaseOperationCategoryService;
import ru.rgasymov.moneymanager.service.UserService;

@Service
@Slf4j
public class IncomeCategoryService
    extends BaseOperationCategoryService<Income, IncomeCategory> {

  private final IncomeCategoryRepository incomeCategoryRepository;

  private final IncomeCategoryMapper incomeCategoryMapper;

  private final CacheManager cacheManager;

  public IncomeCategoryService(
      IncomeCategoryRepository incomeCategoryRepository,
      IncomeRepository incomeRepository,
      IncomeCategoryMapper incomeCategoryMapper,
      UserService userService,
      CacheManager cacheManager) {
    super(incomeRepository, incomeCategoryRepository, incomeCategoryMapper, userService);
    this.incomeCategoryRepository = incomeCategoryRepository;
    this.incomeCategoryMapper = incomeCategoryMapper;
    this.cacheManager = cacheManager;
  }

  @Cacheable(cacheNames = CacheNames.INCOME_CATEGORIES)
  @Transactional(readOnly = true)
  @Override
  public List<OperationCategoryResponseDto> findAll(Long accountId) {
    var result = incomeCategoryRepository.findAll(
        accountIdEq(accountId),
        Sort.by(Sort.Order.asc(IncomeCategory_.NAME).ignoreCase())
    );
    return incomeCategoryMapper.toDtos(result);
  }

  @Cacheable(cacheNames = CacheNames.INCOME_CATEGORIES)
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
  protected IncomeCategory buildNewOperationCategory(User currentUser, String name) {
    return IncomeCategory.builder()
        .accountId(currentUser.getCurrentAccount().getId())
        .name(name)
        .build();
  }

  @Override
  public void clearCachedCategories() {
    Optional.ofNullable(cacheManager.getCache(CacheNames.INCOME_CATEGORIES))
        .ifPresent(Cache::clear);
  }
}
