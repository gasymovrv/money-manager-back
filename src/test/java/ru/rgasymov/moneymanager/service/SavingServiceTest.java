package ru.rgasymov.moneymanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import ru.rgasymov.moneymanager.domain.dto.request.SavingCriteriaDto;
import ru.rgasymov.moneymanager.domain.entity.Account;
import ru.rgasymov.moneymanager.domain.entity.Saving;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;
import ru.rgasymov.moneymanager.domain.enums.Period;
import ru.rgasymov.moneymanager.domain.enums.SavingFieldToSort;
import ru.rgasymov.moneymanager.mapper.SavingGroupMapper;
import ru.rgasymov.moneymanager.mapper.SavingMapper;
import ru.rgasymov.moneymanager.repository.ExpenseRepository;
import ru.rgasymov.moneymanager.repository.IncomeRepository;
import ru.rgasymov.moneymanager.repository.SavingRepository;
import ru.rgasymov.moneymanager.service.expense.ExpenseCategoryService;
import ru.rgasymov.moneymanager.service.income.IncomeCategoryService;

@ExtendWith(MockitoExtension.class)
class SavingServiceTest {

  @Mock
  private SavingRepository savingRepository;

  @Mock
  private IncomeRepository incomeRepository;

  @Mock
  private ExpenseRepository expenseRepository;

  @Mock
  private SavingMapper savingMapper;

  @Mock
  private SavingGroupMapper savingGroupMapper;

  @Mock
  private UserService userService;

  @Mock
  private IncomeCategoryService incomeCategoryService;

  @Mock
  private ExpenseCategoryService expenseCategoryService;

  private SavingService savingService;

  @BeforeEach
  void setUp() {
    savingService = new SavingService(
        savingRepository,
        incomeRepository,
        expenseRepository,
        savingMapper,
        savingGroupMapper,
        userService,
        incomeCategoryService,
        expenseCategoryService
    );
  }

  @Test
  void search_shouldReturnPaginatedSavings() {
    var user = createTestUser();
    var criteria = createCriteria();
    var saving = Saving.builder().id(1L).date(LocalDate.now()).value(BigDecimal.valueOf(1000)).build();
    var page = new PageImpl<>(List.of(saving), PageRequest.of(0, 10), 1);

    when(userService.getCurrentUser()).thenReturn(user);
    when(incomeCategoryService.findAllAndSetChecked(anyLong(), any())).thenReturn(List.of());
    when(expenseCategoryService.findAllAndSetChecked(anyLong(), any())).thenReturn(List.of());
    when(savingRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
    when(savingMapper.toDtos(any())).thenReturn(List.of());

    var result = savingService.search(criteria);

    assertThat(result).isNotNull();
    assertThat(result.getTotalElements()).isEqualTo(1);
    verify(savingRepository).findAll(any(Specification.class), any(PageRequest.class));
  }

  @Test
  void search_shouldGroupByPeriod_whenNotDay() {
    var user = createTestUser();
    var criteria = createCriteria();
    criteria.setGroupBy(Period.MONTH);
    var saving = Saving.builder().id(1L).date(LocalDate.now()).value(BigDecimal.valueOf(1000)).build();
    var page = new PageImpl<>(List.of(saving), PageRequest.of(0, 10), 1);

    when(userService.getCurrentUser()).thenReturn(user);
    when(incomeCategoryService.findAllAndSetChecked(anyLong(), any())).thenReturn(List.of());
    when(expenseCategoryService.findAllAndSetChecked(anyLong(), any())).thenReturn(List.of());
    when(savingRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
    when(savingGroupMapper.toGroupDtos(any(), eq(Period.MONTH))).thenReturn(List.of());

    var result = savingService.search(criteria);

    assertThat(result).isNotNull();
    verify(savingGroupMapper).toGroupDtos(any(), eq(Period.MONTH));
  }

  @Test
  void findByDate_shouldReturnSaving_whenExists() {
    var user = createTestUser();
    var date = LocalDate.now();
    var saving = Saving.builder().id(1L).date(date).value(BigDecimal.valueOf(1000)).build();

    when(userService.getCurrentUser()).thenReturn(user);
    when(savingRepository.findByDateAndAccountId(date, 1L)).thenReturn(Optional.of(saving));

    var result = savingService.findByDate(date);

    assertThat(result).isEqualTo(saving);
  }

  @Test
  void findByDate_shouldThrowException_whenNotFound() {
    var user = createTestUser();
    var date = LocalDate.now();

    when(userService.getCurrentUser()).thenReturn(user);
    when(savingRepository.findByDateAndAccountId(date, 1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> savingService.findByDate(date))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("Could not find saving by date");
  }

  @Test
  void increase_shouldIncreaseSavingValue() {
    var user = createTestUser();
    var date = LocalDate.now();
    var saving = Saving.builder().id(1L).date(date).value(BigDecimal.valueOf(1000)).accountId(1L).build();

    when(userService.getCurrentUser()).thenReturn(user);
    when(savingRepository.findByDateAndAccountId(date, 1L)).thenReturn(Optional.of(saving));

    savingService.increase(BigDecimal.valueOf(500), date);

    assertThat(saving.getValue()).isEqualByComparingTo(BigDecimal.valueOf(1500));
    verify(savingRepository).save(saving);
  }

  @Test
  void increase_shouldCreateNewSaving_whenNotExists() {
    var user = createTestUser();
    var date = LocalDate.now();
    var previousDate = date.minusDays(1);
    var previousSaving = Saving.builder().id(1L).date(previousDate).value(BigDecimal.valueOf(1000)).accountId(1L).build();

    when(userService.getCurrentUser()).thenReturn(user);
    when(savingRepository.findByDateAndAccountId(date, 1L)).thenReturn(Optional.empty());
    when(savingRepository.findFirstByDateLessThanAndAccountIdOrderByDateDesc(date, 1L))
        .thenReturn(Optional.of(previousSaving));

    savingService.increase(BigDecimal.valueOf(500), date);

    verify(savingRepository).save(any(Saving.class));
  }

  @Test
  void decrease_shouldDecreaseSavingValue() {
    var user = createTestUser();
    var date = LocalDate.now();
    var saving = Saving.builder().id(1L).date(date).value(BigDecimal.valueOf(1000)).accountId(1L).build();

    when(userService.getCurrentUser()).thenReturn(user);
    when(savingRepository.findByDateAndAccountId(date, 1L)).thenReturn(Optional.of(saving));

    savingService.decrease(BigDecimal.valueOf(300), date);

    assertThat(saving.getValue()).isEqualByComparingTo(BigDecimal.valueOf(700));
    verify(savingRepository).save(saving);
  }

  @Test
  void updateAfterDeletionOperation_shouldDeleteSaving_whenNoOperations() {
    var user = createTestUser();
    var date = LocalDate.now();
    var saving = Saving.builder().id(1L).date(date).value(BigDecimal.valueOf(1000))
        .incomes(List.of())
        .expenses(List.of())
        .build();

    when(userService.getCurrentUser()).thenReturn(user);
    when(savingRepository.findByDateAndAccountId(date, 1L)).thenReturn(Optional.of(saving));

    savingService.updateAfterDeletionOperation(date);

    verify(savingRepository, times(1)).delete(saving);
  }

  @Test
  void updateAfterDeletionOperation_shouldNotDeleteSaving_whenHasOperations() {
    var user = createTestUser();
    var date = LocalDate.now();
    // Saving with at least one operation - won't be deleted
    var income = new ru.rgasymov.moneymanager.domain.entity.Income();
    var saving = Saving.builder()
        .id(1L)
        .date(date)
        .value(BigDecimal.valueOf(1000))
        .incomes(List.of(income))
        .build();

    when(userService.getCurrentUser()).thenReturn(user);
    when(savingRepository.findByDateAndAccountId(date, 1L)).thenReturn(Optional.of(saving));

    savingService.updateAfterDeletionOperation(date);

    verify(savingRepository, never()).delete((Saving) any());
  }

  private User createTestUser() {
    var account = Account.builder()
        .id(1L)
        .name("Test Account")
        .theme(AccountTheme.LIGHT)
        .currency("USD")
        .build();

    return User.builder()
        .id("user123")
        .email("test@example.com")
        .name("Test User")
        .provider(AuthProviders.GOOGLE)
        .currentAccount(account)
        .build();
  }

  private SavingCriteriaDto createCriteria() {
    var criteria = new SavingCriteriaDto();
    criteria.setPageNum(0);
    criteria.setPageSize(10);
    criteria.setSortBy(SavingFieldToSort.DATE);
    criteria.setSortDirection(Sort.Direction.DESC);
    criteria.setGroupBy(Period.DAY);
    criteria.setFrom(LocalDate.now().minusMonths(1));
    criteria.setTo(LocalDate.now());
    return criteria;
  }
}
