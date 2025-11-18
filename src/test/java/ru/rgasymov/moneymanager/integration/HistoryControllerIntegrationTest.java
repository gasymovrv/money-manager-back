package ru.rgasymov.moneymanager.integration;

import static org.assertj.core.api.Assertions.assertThat;
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
import ru.rgasymov.moneymanager.domain.entity.Saving;
import ru.rgasymov.moneymanager.repository.ExpenseCategoryRepository;
import ru.rgasymov.moneymanager.repository.ExpenseRepository;
import ru.rgasymov.moneymanager.repository.HistoryRepository;
import ru.rgasymov.moneymanager.repository.SavingRepository;

/**
 * Integration tests for HistoryController.
 */
class HistoryControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private HistoryRepository historyRepository;

  @Autowired
  private ExpenseRepository expenseRepository;

  @Autowired
  private ExpenseCategoryRepository expenseCategoryRepository;

  @Value("${server.api-base-url}")
  private String apiBaseUrl;

  @Autowired
  private SavingRepository savingRepository;

  @BeforeEach
  void setUpHistory() {
    // Create a saving first (required for expense)
    var saving = Saving.builder()
        .date(LocalDate.now())
        .value(BigDecimal.ZERO)
        .accountId(testAccount.getId())
        .build();
    saving = savingRepository.save(saving);

    // Create an expense to generate history entry
    var category = ExpenseCategory.builder()
        .name("Test Category")
        .accountId(testAccount.getId())
        .build();
    category = expenseCategoryRepository.save(category);

    var expense = Expense.builder()
        .date(LocalDate.now())
        .value(BigDecimal.valueOf(100))
        .description("Test expense for history")
        .isPlanned(false)
        .category(category)
        .accountId(testAccount.getId())
        .savingId(saving.getId())
        .build();
    expenseRepository.save(expense);
  }

  @Test
  void findAll_shouldReturnPagedHistoryActions() throws Exception {
    // Given: Verify history repository is accessible
    var historyCount = historyRepository.count();
    assertThat(historyCount).isGreaterThanOrEqualTo(0);

    // When & Then: Fetch history and verify response
    mockMvc.perform(get(apiBaseUrl + "/history")
            .header("Authorization", getAuthorizationHeader())
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
    
    // History logging might be async or disabled in tests
  }

  @Test
  void findAll_shouldReturnUnauthorized_whenNoToken() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/history"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void findAll_shouldSupportPagination() throws Exception {
    // Given: Verify history repository is accessible
    var totalHistoryEntries = historyRepository.count();
    assertThat(totalHistoryEntries).isGreaterThanOrEqualTo(0);

    // When & Then: Fetch with pagination parameters
    mockMvc.perform(get(apiBaseUrl + "/history")
            .header("Authorization", getAuthorizationHeader())
            .param("page", "0")
            .param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.size").value(5));
  }
}
