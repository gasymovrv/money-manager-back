package ru.rgasymov.moneymanager.service.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rgasymov.moneymanager.domain.dto.request.OperationRequestDto;
import ru.rgasymov.moneymanager.domain.entity.Account;
import ru.rgasymov.moneymanager.domain.entity.Expense;
import ru.rgasymov.moneymanager.domain.entity.ExpenseCategory;
import ru.rgasymov.moneymanager.domain.entity.Saving;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;
import ru.rgasymov.moneymanager.mapper.ExpenseMapper;
import ru.rgasymov.moneymanager.repository.ExpenseCategoryRepository;
import ru.rgasymov.moneymanager.repository.ExpenseRepository;
import ru.rgasymov.moneymanager.service.HistoryService;
import ru.rgasymov.moneymanager.service.SavingService;
import ru.rgasymov.moneymanager.service.UserService;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

  @Mock
  private ExpenseRepository expenseRepository;

  @Mock
  private ExpenseCategoryRepository expenseCategoryRepository;

  @Mock
  private ExpenseMapper expenseMapper;

  @Mock
  private UserService userService;

  @Mock
  private SavingService savingService;

  @Mock
  private HistoryService historyService;

  private ExpenseService expenseService;

  @BeforeEach
  void setUp() {
    expenseService = new ExpenseService(
        expenseRepository,
        expenseCategoryRepository,
        expenseMapper,
        userService,
        savingService,
        historyService
    );
  }

  @Test
  void buildNewOperation_shouldCreateExpenseWithCorrectFields() {
    var user = createTestUser();
    var category = ExpenseCategory.builder().id(1L).name("Food").accountId(1L).build();
    var dto = new OperationRequestDto();
    dto.setDate(LocalDate.now());
    dto.setValue(BigDecimal.valueOf(100));
    dto.setDescription("Groceries");
    dto.setIsPlanned(false);
    dto.setCategoryId(1L);

    when(userService.getCurrentUser()).thenReturn(user);

    var expense = expenseService.buildNewOperation(dto, category);

    assertThat(expense).isNotNull();
    assertThat(expense.getDate()).isEqualTo(dto.getDate());
    assertThat(expense.getValue()).isEqualTo(dto.getValue());
    assertThat(expense.getDescription()).isEqualTo(dto.getDescription());
    assertThat(expense.getIsPlanned()).isEqualTo(dto.getIsPlanned());
    assertThat(expense.getCategory()).isEqualTo(category);
    assertThat(expense.getAccountId()).isEqualTo(1L);
  }

  @Test
  void cloneOperation_shouldCreateCopyOfExpense() {
    var expense = Expense.builder()
        .id(1L)
        .date(LocalDate.now())
        .value(BigDecimal.valueOf(100))
        .description("Test")
        .isPlanned(false)
        .accountId(1L)
        .build();

    var cloned = expenseService.cloneOperation(expense);

    assertThat(cloned).isNotNull();
    assertThat(cloned.getDate()).isEqualTo(expense.getDate());
    assertThat(cloned.getValue()).isEqualTo(expense.getValue());
    assertThat(cloned.getDescription()).isEqualTo(expense.getDescription());
  }

  @Test
  void updateSavings_shouldDecreaseSavingsWhenExpenseIncreases() {
    var expense = Expense.builder().id(1L).accountId(1L).build();
    var saving = Saving.builder().id(1L).value(BigDecimal.valueOf(1000)).build();
    var date = LocalDate.now();

    when(savingService.findByDate(date)).thenReturn(saving);

    expenseService.updateSavings(BigDecimal.valueOf(150), BigDecimal.valueOf(100), date, expense);

    verify(savingService).decrease(BigDecimal.valueOf(50), date);
    verify(savingService).findByDate(date);
  }

  @Test
  void updateSavings_shouldIncreaseSavingsWhenExpenseDecreases() {
    var expense = Expense.builder().id(1L).accountId(1L).build();
    var saving = Saving.builder().id(1L).value(BigDecimal.valueOf(1000)).build();
    var date = LocalDate.now();

    when(savingService.findByDate(date)).thenReturn(saving);

    expenseService.updateSavings(BigDecimal.valueOf(80), BigDecimal.valueOf(100), date, expense);

    verify(savingService).increase(BigDecimal.valueOf(20), date);
    verify(savingService).findByDate(date);
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
}
