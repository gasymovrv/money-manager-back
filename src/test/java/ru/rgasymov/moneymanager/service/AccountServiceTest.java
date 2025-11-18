package ru.rgasymov.moneymanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rgasymov.moneymanager.domain.dto.request.AccountRequestDto;
import ru.rgasymov.moneymanager.domain.dto.response.AccountResponseDto;
import ru.rgasymov.moneymanager.domain.entity.Account;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;
import ru.rgasymov.moneymanager.mapper.AccountMapper;
import ru.rgasymov.moneymanager.repository.AccountRepository;
import ru.rgasymov.moneymanager.repository.ExpenseCategoryRepository;
import ru.rgasymov.moneymanager.repository.ExpenseRepository;
import ru.rgasymov.moneymanager.repository.HistoryRepository;
import ru.rgasymov.moneymanager.repository.IncomeCategoryRepository;
import ru.rgasymov.moneymanager.repository.IncomeRepository;
import ru.rgasymov.moneymanager.repository.SavingRepository;
import ru.rgasymov.moneymanager.service.expense.ExpenseCategoryService;
import ru.rgasymov.moneymanager.service.income.IncomeCategoryService;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock
  private AccountRepository accountRepository;
  @Mock
  private IncomeRepository incomeRepository;
  @Mock
  private IncomeCategoryRepository incomeCategoryRepository;
  @Mock
  private ExpenseRepository expenseRepository;
  @Mock
  private ExpenseCategoryRepository expenseCategoryRepository;
  @Mock
  private SavingRepository savingRepository;
  @Mock
  private HistoryRepository historyRepository;
  @Mock
  private AccountMapper accountMapper;
  @Mock
  private UserService userService;
  @Mock
  private ExpenseCategoryService expenseCategoryService;
  @Mock
  private IncomeCategoryService incomeCategoryService;

  private AccountService accountService;

  @BeforeEach
  void setUp() {
    accountService = new AccountService(
        accountRepository,
        incomeRepository,
        incomeCategoryRepository,
        expenseRepository,
        expenseCategoryRepository,
        savingRepository,
        historyRepository,
        accountMapper,
        userService,
        expenseCategoryService,
        incomeCategoryService
    );
  }

  @Test
  void findAll_shouldReturnAllAccountsForCurrentUser() {
    var user = createTestUser();
    var accounts = List.of(createTestAccount(1L), createTestAccount(2L));
    var dtos = List.of(new AccountResponseDto(), new AccountResponseDto());

    when(userService.getCurrentUser()).thenReturn(user);
    when(accountRepository.findAllByUserId("user123")).thenReturn(accounts);
    when(accountMapper.toDtos(accounts)).thenReturn(dtos);

    var result = accountService.findAll();

    assertThat(result).hasSize(2);
    verify(accountRepository).findAllByUserId("user123");
  }

  @Test
  void create_shouldCreateNewAccount() {
    var user = createTestUser();
    var dto = new AccountRequestDto();
    dto.setName("New Account");
    dto.setTheme(AccountTheme.DARK);
    dto.setCurrency("EUR");
    var account = createTestAccount(null);
    var savedAccount = createTestAccount(3L);
    var responseDto = new AccountResponseDto();

    when(userService.getCurrentUser()).thenReturn(user);
    when(accountMapper.fromDto(dto)).thenReturn(account);
    when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
    when(accountMapper.toDto(savedAccount)).thenReturn(responseDto);

    var result = accountService.create(dto);

    assertThat(result).isNotNull();
    verify(accountRepository).save(any(Account.class));
  }

  @Test
  void update_shouldUpdateAccountFields() {
    var user = createTestUser();
    var account = createTestAccount(1L);
    var dto = new AccountRequestDto();
    dto.setName("Updated Account");
    dto.setTheme(AccountTheme.DARK);
    dto.setCurrency("EUR");
    var responseDto = new AccountResponseDto();

    when(userService.getCurrentUser()).thenReturn(user);
    when(accountRepository.findByIdAndUserId(1L, "user123"))
        .thenReturn(Optional.of(account));
    when(accountMapper.toDto(account)).thenReturn(responseDto);

    var result = accountService.update(1L, dto);

    assertThat(result).isNotNull();
    verify(accountRepository).save(account);
  }

  @Test
  void update_shouldNotSave_whenNoChanges() {
    var user = createTestUser();
    var account = createTestAccount(1L);
    var dto = new AccountRequestDto();
    dto.setName("Test Account");
    dto.setTheme(AccountTheme.LIGHT);
    dto.setCurrency("USD");
    var responseDto = new AccountResponseDto();

    when(userService.getCurrentUser()).thenReturn(user);
    when(accountRepository.findByIdAndUserId(1L, "user123"))
        .thenReturn(Optional.of(account));
    when(accountMapper.toDto(account)).thenReturn(responseDto);

    var result = accountService.update(1L, dto);

    assertThat(result).isNotNull();
    verify(accountRepository, never()).save(any());
  }

  @Test
  void delete_shouldDeleteAccountAndRelatedData() {
    var user = createTestUser();
    var account = createTestAccount(2L);

    when(userService.getCurrentUser()).thenReturn(user);
    when(accountRepository.findByIdAndUserId(2L, "user123"))
        .thenReturn(Optional.of(account));

    accountService.delete(2L);

    verify(expenseRepository).deleteAllByAccountId(2L);
    verify(incomeRepository).deleteAllByAccountId(2L);
    verify(incomeCategoryRepository).deleteAllByAccountId(2L);
    verify(expenseCategoryRepository).deleteAllByAccountId(2L);
    verify(savingRepository).deleteAllByAccountId(2L);
    verify(historyRepository).deleteAllByAccountId(2L);
    verify(accountRepository).deleteById(2L);
  }

  @Test
  void delete_shouldThrowException_whenDeletingCurrentAccount() {
    var user = createTestUser();

    when(userService.getCurrentUser()).thenReturn(user);

    assertThatThrownBy(() -> accountService.delete(1L))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("Cannot delete current account");
  }

  @Test
  void delete_shouldThrowException_whenAccountNotFound() {
    var user = createTestUser();

    when(userService.getCurrentUser()).thenReturn(user);
    when(accountRepository.findByIdAndUserId(999L, "user123"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountService.delete(999L))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void changeCurrent_shouldUpdateCurrentAccount() {
    var user = createTestUser();
    var newAccount = createTestAccount(2L);
    var responseDto = new AccountResponseDto();

    when(userService.getCurrentUser()).thenReturn(user);
    when(accountRepository.findByIdAndUserId(2L, "user123"))
        .thenReturn(Optional.of(newAccount));
    when(accountMapper.toDto(newAccount)).thenReturn(responseDto);

    var result = accountService.changeCurrent(2L);

    assertThat(result).isNotNull();
    verify(userService).save(user);
    assertThat(user.getCurrentAccount()).isEqualTo(newAccount);
  }

  @Test
  void createDefaultCategories_shouldCreateDefaultIncomeAndExpenseCategories() {
    accountService.createDefaultCategories();

    verify(incomeCategoryService, org.mockito.Mockito.times(2)).create(any());
    verify(expenseCategoryService, org.mockito.Mockito.times(5)).create(any());
  }

  @Test
  void isCurrentAccountEmpty_shouldReturnTrue_whenAccountHasNoData() {
    var user = createTestUser();

    when(userService.getCurrentUser()).thenReturn(user);
    when(savingRepository.existsByAccountId(1L)).thenReturn(false);
    when(incomeCategoryRepository.existsByAccountId(1L)).thenReturn(false);
    when(expenseCategoryRepository.existsByAccountId(1L)).thenReturn(false);

    var result = accountService.isCurrentAccountEmpty();

    assertThat(result).isTrue();
  }

  @Test
  void isCurrentAccountEmpty_shouldReturnFalse_whenAccountHasData() {
    var user = createTestUser();

    when(userService.getCurrentUser()).thenReturn(user);
    when(savingRepository.existsByAccountId(1L)).thenReturn(true);

    var result = accountService.isCurrentAccountEmpty();

    assertThat(result).isFalse();
  }

  private User createTestUser() {
    var account = createTestAccount(1L);
    var user = User.builder()
        .id("user123")
        .email("test@example.com")
        .name("Test User")
        .provider(AuthProviders.GOOGLE)
        .currentAccount(account)
        .build();
    account.setUser(user);
    return user;
  }

  private Account createTestAccount(Long id) {
    return Account.builder()
        .id(id)
        .name("Test Account")
        .theme(AccountTheme.LIGHT)
        .currency("USD")
        .build();
  }
}
