package ru.rgasymov.moneymanager.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.rgasymov.moneymanager.domain.entity.Expense;
import ru.rgasymov.moneymanager.domain.entity.ExpenseCategory;
import ru.rgasymov.moneymanager.domain.entity.Income;
import ru.rgasymov.moneymanager.domain.entity.IncomeCategory;
import ru.rgasymov.moneymanager.domain.entity.Saving;
import ru.rgasymov.moneymanager.repository.ExpenseCategoryRepository;
import ru.rgasymov.moneymanager.repository.ExpenseRepository;
import ru.rgasymov.moneymanager.repository.IncomeCategoryRepository;
import ru.rgasymov.moneymanager.repository.IncomeRepository;
import ru.rgasymov.moneymanager.repository.SavingRepository;

/**
 * Integration tests for SavingController.
 */
class SavingControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private SavingRepository savingRepository;

  @Autowired
  private IncomeRepository incomeRepository;

  @Autowired
  private ExpenseRepository expenseRepository;

  @Autowired
  private IncomeCategoryRepository incomeCategoryRepository;

  @Autowired
  private ExpenseCategoryRepository expenseCategoryRepository;

  @Value("${server.api-base-url}")
  private String apiBaseUrl;

  private Saving testSaving;
  private IncomeCategory incomeCategory;
  private ExpenseCategory expenseCategory;

  @BeforeEach
  void setUp() {
    testSaving = Saving.builder()
        .date(LocalDate.now())
        .value(BigDecimal.valueOf(5000))
        .accountId(testAccount.getId())
        .build();
    testSaving = savingRepository.save(testSaving);

    incomeCategory = IncomeCategory.builder()
        .name("Salary")
        .accountId(testAccount.getId())
        .build();
    incomeCategory = incomeCategoryRepository.save(incomeCategory);

    expenseCategory = ExpenseCategory.builder()
        .name("Food")
        .accountId(testAccount.getId())
        .build();
    expenseCategory = expenseCategoryRepository.save(expenseCategory);

    var income = Income.builder()
        .date(LocalDate.now())
        .value(BigDecimal.valueOf(5000))
        .description("Monthly salary")
        .isPlanned(false)
        .category(incomeCategory)
        .accountId(testAccount.getId())
        .savingId(testSaving.getId())
        .build();
    incomeRepository.save(income);

    var expense = Expense.builder()
        .date(LocalDate.now())
        .value(BigDecimal.valueOf(100))
        .description("Groceries")
        .isPlanned(false)
        .category(expenseCategory)
        .accountId(testAccount.getId())
        .savingId(testSaving.getId())
        .build();
    expenseRepository.save(expense);
  }

  @Test
  void search_shouldReturnSavingsWithDefaultCriteria() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/savings")
            .header("Authorization", getAuthorizationHeader())
            .param("pageNum", "0")
            .param("pageSize", "10")
            .param("sortBy", "DATE")
            .param("sortDirection", "DESC")
            .param("groupBy", "DAY"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").isArray())
        .andExpect(jsonPath("$.totalElements").isNumber());
  }

  @Test
  void search_shouldReturnSavingsWithDateRange() throws Exception {
    var from = LocalDate.now().minusDays(7);
    var to = LocalDate.now();

    mockMvc.perform(get(apiBaseUrl + "/savings")
            .header("Authorization", getAuthorizationHeader())
            .param("pageNum", "0")
            .param("pageSize", "10")
            .param("sortBy", "DATE")
            .param("sortDirection", "DESC")
            .param("groupBy", "DAY")
            .param("dateFrom", from.toString())
            .param("dateTo", to.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").isArray());
  }

  @Test
  void search_shouldReturnSavingsGroupedByMonth() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/savings")
            .header("Authorization", getAuthorizationHeader())
            .param("pageNum", "0")
            .param("pageSize", "10")
            .param("sortBy", "DATE")
            .param("sortDirection", "DESC")
            .param("groupBy", "MONTH"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").isArray());
  }

  @Test
  void search_shouldReturnIncomeCategories() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/savings")
            .header("Authorization", getAuthorizationHeader())
            .param("pageNum", "0")
            .param("pageSize", "10")
            .param("sortBy", "DATE")
            .param("sortDirection", "DESC")
            .param("groupBy", "DAY"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.incomeCategories").isArray());
  }

  @Test
  void search_shouldReturnExpenseCategories() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/savings")
            .header("Authorization", getAuthorizationHeader())
            .param("pageNum", "0")
            .param("pageSize", "10")
            .param("sortBy", "DATE")
            .param("sortDirection", "DESC")
            .param("groupBy", "DAY"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.expenseCategories").isArray());
  }

  @Test
  void search_shouldFilterByCategories() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/savings")
            .header("Authorization", getAuthorizationHeader())
            .param("pageNum", "0")
            .param("pageSize", "10")
            .param("sortBy", "DATE")
            .param("sortDirection", "DESC")
            .param("groupBy", "DAY")
            .param("incomeCategoryIds", incomeCategory.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").isArray());
  }

  @Test
  void search_shouldReturnUnauthorized_whenNoToken() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/savings"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void search_shouldSupportSearchText() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/savings")
            .header("Authorization", getAuthorizationHeader())
            .param("pageNum", "0")
            .param("pageSize", "10")
            .param("sortBy", "DATE")
            .param("sortDirection", "DESC")
            .param("groupBy", "DAY")
            .param("searchText", "salary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").isArray());
  }

  @Test
  void search_shouldSupportPagination() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/savings")
            .header("Authorization", getAuthorizationHeader())
            .param("pageNum", "0")
            .param("pageSize", "5")
            .param("sortBy", "DATE")
            .param("sortDirection", "DESC")
            .param("groupBy", "DAY"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").isArray());
  }
}
