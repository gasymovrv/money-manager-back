package ru.rgasymov.moneymanager.service.income;

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
import ru.rgasymov.moneymanager.domain.entity.IncomeCategory;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;
import ru.rgasymov.moneymanager.mapper.IncomeCategoryMapper;
import ru.rgasymov.moneymanager.repository.IncomeCategoryRepository;
import ru.rgasymov.moneymanager.repository.IncomeRepository;
import ru.rgasymov.moneymanager.service.UserService;

@ExtendWith(MockitoExtension.class)
class IncomeCategoryServiceTest {

  @Mock
  private IncomeCategoryRepository incomeCategoryRepository;

  @Mock
  private IncomeRepository incomeRepository;

  @Mock
  private IncomeCategoryMapper incomeCategoryMapper;

  @Mock
  private UserService userService;

  @Mock
  private CacheManager cacheManager;

  @Mock
  private Cache cache;

  private IncomeCategoryService service;

  @BeforeEach
  void setUp() {
    service = new IncomeCategoryService(
        incomeCategoryRepository,
        incomeRepository,
        incomeCategoryMapper,
        userService,
        cacheManager
    );
  }

  @Test
  void findAll_shouldReturnCategoriesSortedByName() {
    var category1 = IncomeCategory.builder().id(1L).name("Salary").accountId(1L).build();
    var category2 = IncomeCategory.builder().id(2L).name("Bonus").accountId(1L).build();
    var dto1 = new OperationCategoryResponseDto();
    dto1.setId(1L);
    dto1.setName("Salary");
    var dto2 = new OperationCategoryResponseDto();
    dto2.setId(2L);
    dto2.setName("Bonus");

    when(incomeCategoryRepository.findAll(any(Specification.class), any(Sort.class)))
        .thenReturn(List.of(category1, category2));
    when(incomeCategoryMapper.toDtos(any())).thenReturn(List.of(dto1, dto2));

    var result = service.findAll(1L);

    assertThat(result).hasSize(2);
    verify(incomeCategoryRepository).findAll(any(Specification.class), any(Sort.class));
  }

  @Test
  void findAllAndSetChecked_shouldMarkSelectedCategories() {
    var dto1 = new OperationCategoryResponseDto();
    dto1.setId(1L);
    var dto2 = new OperationCategoryResponseDto();
    dto2.setId(2L);

    when(incomeCategoryRepository.findAll(any(Specification.class), any(Sort.class)))
        .thenReturn(List.of());
    when(incomeCategoryMapper.toDtos(any())).thenReturn(List.of(dto1, dto2));

    var result = service.findAllAndSetChecked(1L, List.of(2L));

    assertThat(result.get(0).isChecked()).isFalse();
    assertThat(result.get(1).isChecked()).isTrue();
  }

  @Test
  void buildNewOperationCategory_shouldCreateCategoryForCurrentAccount() {
    var account = Account.builder().id(5L).name("Test").theme(AccountTheme.LIGHT).currency("EUR").build();
    var user = User.builder()
        .id("user1")
        .email("test@test.com")
        .name("Test")
        .provider(AuthProviders.GOOGLE)
        .currentAccount(account)
        .build();

    var category = service.buildNewOperationCategory(user, "Freelance");

    assertThat(category.getName()).isEqualTo("Freelance");
    assertThat(category.getAccountId()).isEqualTo(5L);
  }

  @Test
  void clearCachedCategories_shouldClearCache() {
    when(cacheManager.getCache("incomeCategories")).thenReturn(cache);

    service.clearCachedCategories();

    verify(cache).clear();
  }

  @Test
  void findByIdAndAccountId_shouldReturnCategory_whenFound() {
    var category = IncomeCategory.builder().id(10L).name("Category").accountId(1L).build();
    var dto = new OperationCategoryResponseDto();
    dto.setId(10L);

    when(incomeCategoryRepository.findByIdAndAccountId(10L, 1L))
        .thenReturn(Optional.of(category));
    when(incomeCategoryMapper.toDto(category)).thenReturn(dto);

    var result = service.findByIdAndAccountId(10L, 1L);

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(10L);
  }
}
