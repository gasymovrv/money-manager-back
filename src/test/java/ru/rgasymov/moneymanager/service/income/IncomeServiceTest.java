package ru.rgasymov.moneymanager.service.income;

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
import ru.rgasymov.moneymanager.domain.entity.Income;
import ru.rgasymov.moneymanager.domain.entity.IncomeCategory;
import ru.rgasymov.moneymanager.domain.entity.Saving;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;
import ru.rgasymov.moneymanager.mapper.IncomeMapper;
import ru.rgasymov.moneymanager.repository.IncomeCategoryRepository;
import ru.rgasymov.moneymanager.repository.IncomeRepository;
import ru.rgasymov.moneymanager.service.HistoryService;
import ru.rgasymov.moneymanager.service.SavingService;
import ru.rgasymov.moneymanager.service.UserService;

@ExtendWith(MockitoExtension.class)
class IncomeServiceTest {

  @Mock
  private IncomeRepository incomeRepository;

  @Mock
  private IncomeCategoryRepository incomeCategoryRepository;

  @Mock
  private IncomeMapper incomeMapper;

  @Mock
  private UserService userService;

  @Mock
  private SavingService savingService;

  @Mock
  private HistoryService historyService;

  private IncomeService incomeService;

  @BeforeEach
  void setUp() {
    incomeService = new IncomeService(
        incomeRepository,
        incomeCategoryRepository,
        incomeMapper,
        userService,
        savingService,
        historyService
    );
  }

  @Test
  void buildNewOperation_shouldCreateIncomeWithCorrectFields() {
    var user = createTestUser();
    var category = IncomeCategory.builder().id(1L).name("Salary").accountId(1L).build();
    var dto = new OperationRequestDto();
    dto.setDate(LocalDate.now());
    dto.setValue(BigDecimal.valueOf(5000));
    dto.setDescription("Monthly salary");
    dto.setIsPlanned(false);
    dto.setCategoryId(1L);

    when(userService.getCurrentUser()).thenReturn(user);

    var income = incomeService.buildNewOperation(dto, category);

    assertThat(income).isNotNull();
    assertThat(income.getDate()).isEqualTo(dto.getDate());
    assertThat(income.getValue()).isEqualTo(dto.getValue());
    assertThat(income.getDescription()).isEqualTo(dto.getDescription());
    assertThat(income.getIsPlanned()).isEqualTo(dto.getIsPlanned());
    assertThat(income.getCategory()).isEqualTo(category);
    assertThat(income.getAccountId()).isEqualTo(1L);
  }

  @Test
  void cloneOperation_shouldCreateCopyOfIncome() {
    var income = Income.builder()
        .id(1L)
        .date(LocalDate.now())
        .value(BigDecimal.valueOf(5000))
        .description("Salary")
        .isPlanned(false)
        .accountId(1L)
        .build();

    var cloned = incomeService.cloneOperation(income);

    assertThat(cloned).isNotNull();
    assertThat(cloned.getDate()).isEqualTo(income.getDate());
    assertThat(cloned.getValue()).isEqualTo(income.getValue());
    assertThat(cloned.getDescription()).isEqualTo(income.getDescription());
  }

  @Test
  void updateSavings_shouldIncreaseSavingsWhenIncomeIncreases() {
    var income = Income.builder().id(1L).accountId(1L).build();
    var saving = Saving.builder().id(1L).value(BigDecimal.valueOf(1000)).build();
    var date = LocalDate.now();

    when(savingService.findByDate(date)).thenReturn(saving);

    incomeService.updateSavings(BigDecimal.valueOf(6000), BigDecimal.valueOf(5000), date, income);

    verify(savingService).increase(BigDecimal.valueOf(1000), date);
    verify(savingService).findByDate(date);
  }

  @Test
  void updateSavings_shouldDecreaseSavingsWhenIncomeDecreases() {
    var income = Income.builder().id(1L).accountId(1L).build();
    var saving = Saving.builder().id(1L).value(BigDecimal.valueOf(1000)).build();
    var date = LocalDate.now();

    when(savingService.findByDate(date)).thenReturn(saving);

    incomeService.updateSavings(BigDecimal.valueOf(4000), BigDecimal.valueOf(5000), date, income);

    verify(savingService).decrease(BigDecimal.valueOf(1000), date);
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
