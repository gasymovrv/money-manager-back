package ru.rgasymov.moneymanager.service.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import ru.rgasymov.moneymanager.domain.dto.response.OperationCategoryResponseDto;
import ru.rgasymov.moneymanager.domain.entity.Account;
import ru.rgasymov.moneymanager.domain.entity.ExpenseCategory;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;
import ru.rgasymov.moneymanager.mapper.ExpenseCategoryMapper;
import ru.rgasymov.moneymanager.repository.ExpenseCategoryRepository;
import ru.rgasymov.moneymanager.repository.ExpenseRepository;
import ru.rgasymov.moneymanager.service.UserService;

@ExtendWith(MockitoExtension.class)
class ExpenseCategoryServiceTest {

  @Mock
  private ExpenseCategoryRepository expenseCategoryRepository;

  @Mock
  private ExpenseRepository expenseRepository;

  @Mock
  private ExpenseCategoryMapper expenseCategoryMapper;

  @Mock
  private UserService userService;

  @Mock
  private CacheManager cacheManager;

  @Mock
  private Cache cache;

  private ExpenseCategoryService service;

  @BeforeEach
  void setUp() {
    service = new ExpenseCategoryService(
        expenseCategoryRepository,
        expenseRepository,
        expenseCategoryMapper,
        userService,
        cacheManager
    );
  }

  @Test
  void findAll_shouldReturnCategoriesSortedByName() {
    var category1 = ExpenseCategory.builder().id(1L).name("Category A").accountId(1L).build();
    var category2 = ExpenseCategory.builder().id(2L).name("Category B").accountId(1L).build();
    var dto1 = new OperationCategoryResponseDto();
    dto1.setId(1L);
    dto1.setName("Category A");
    var dto2 = new OperationCategoryResponseDto();
    dto2.setId(2L);
    dto2.setName("Category B");

    when(expenseCategoryRepository.findAll(any(Specification.class), any(Sort.class)))
        .thenReturn(List.of(category1, category2));
    when(expenseCategoryMapper.toDtos(any())).thenReturn(List.of(dto1, dto2));

    var result = service.findAll(1L);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("Category A");
    verify(expenseCategoryRepository).findAll(any(Specification.class), any(Sort.class));
  }

  @Test
  void findAllAndSetChecked_shouldSetCheckedFlag() {
    var dto1 = new OperationCategoryResponseDto();
    dto1.setId(1L);
    dto1.setName("Category A");
    var dto2 = new OperationCategoryResponseDto();
    dto2.setId(2L);
    dto2.setName("Category B");

    when(expenseCategoryRepository.findAll(any(Specification.class), any(Sort.class)))
        .thenReturn(List.of());
    when(expenseCategoryMapper.toDtos(any())).thenReturn(List.of(dto1, dto2));

    var result = service.findAllAndSetChecked(1L, List.of(1L));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).isChecked()).isTrue();
    assertThat(result.get(1).isChecked()).isFalse();
  }

  @Test
  void findByIdAndAccountId_shouldReturnCategory_whenExists() {
    var category = ExpenseCategory.builder().id(1L).name("Category").accountId(1L).build();
    var dto = new OperationCategoryResponseDto();
    dto.setId(1L);

    when(expenseCategoryRepository.findByIdAndAccountId(1L, 1L))
        .thenReturn(Optional.of(category));
    when(expenseCategoryMapper.toDto(category)).thenReturn(dto);

    var result = service.findByIdAndAccountId(1L, 1L);

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(1L);
  }

  @Test
  void findByIdAndAccountId_shouldReturnEmpty_whenNotExists() {
    when(expenseCategoryRepository.findByIdAndAccountId(999L, 1L))
        .thenReturn(Optional.empty());

    var result = service.findByIdAndAccountId(999L, 1L);

    assertThat(result).isEmpty();
  }

  @Test
  void buildNewOperationCategory_shouldCreateCategoryWithCurrentAccount() {
    var account = Account.builder().id(1L).name("Test").theme(AccountTheme.LIGHT).currency("USD").build();
    var user = User.builder()
        .id("user1")
        .email("test@test.com")
        .name("Test")
        .provider(AuthProviders.GOOGLE)
        .currentAccount(account)
        .build();

    var category = service.buildNewOperationCategory(user, "New Category");

    assertThat(category).isNotNull();
    assertThat(category.getName()).isEqualTo("New Category");
    assertThat(category.getAccountId()).isEqualTo(1L);
  }

  @Test
  void clearCachedCategories_shouldClearCache() {
    when(cacheManager.getCache("expenseCategories")).thenReturn(cache);

    service.clearCachedCategories();

    verify(cache).clear();
  }

  @Test
  void clearCachedCategories_shouldHandleNullCache() {
    when(cacheManager.getCache("expenseCategories")).thenReturn(null);

    service.clearCachedCategories();

    // Should not throw exception
  }
}
