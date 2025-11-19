package ru.rgasymov.moneymanager.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import ru.rgasymov.moneymanager.domain.dto.request.OperationCategoryRequestDto;
import ru.rgasymov.moneymanager.domain.dto.request.OperationRequestDto;
import ru.rgasymov.moneymanager.domain.entity.ExpenseCategory;
import ru.rgasymov.moneymanager.domain.entity.Saving;
import ru.rgasymov.moneymanager.repository.ExpenseCategoryRepository;
import ru.rgasymov.moneymanager.repository.ExpenseRepository;
import ru.rgasymov.moneymanager.repository.SavingRepository;

/**
 * Integration tests for ExpenseController.
 * Tests end-to-end flow from HTTP request to database.
 */
class ExpenseControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private ExpenseCategoryRepository expenseCategoryRepository;

  @Autowired
  private ExpenseRepository expenseRepository;

  @Autowired
  private SavingRepository savingRepository;

  @Value("${server.api-base-url}")
  private String apiBaseUrl;

  private ExpenseCategory testCategory;

  @BeforeEach
  void setUp() {
    // Create a test expense category
    testCategory = ExpenseCategory.builder()
        .name("Test Expense Category")
        .accountId(testAccount.getId())
        .build();
    testCategory = expenseCategoryRepository.save(testCategory);

    // Create initial savings
    var saving = Saving.builder()
        .date(LocalDate.now())
        .value(BigDecimal.valueOf(1000))
        .accountId(testAccount.getId())
        .build();
    savingRepository.save(saving);
  }

  @Test
  void create_shouldCreateNewExpense() throws Exception {
    // Given: Verify initial state
    var initialCount = expenseRepository.count();
    var initialSaving = savingRepository.findByDateAndAccountId(LocalDate.now(), testAccount.getId());
    var initialSavingValue = initialSaving.map(Saving::getValue).orElse(BigDecimal.ZERO);

    var dto = new OperationRequestDto();
    dto.setDate(LocalDate.now());
    dto.setValue(BigDecimal.valueOf(100));
    dto.setDescription("Test expense");
    dto.setIsPlanned(false);
    dto.setCategoryId(testCategory.getId());

    // When: Create expense
    mockMvc.perform(post(apiBaseUrl + "/expenses")
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value(100))
        .andExpect(jsonPath("$.description").value("Test expense"));

    // Then: Verify database state after creation
    assertThat(expenseRepository.count()).isEqualTo(initialCount + 1);
    
    var createdExpense = expenseRepository.findAll().stream()
        .filter(e -> "Test expense".equals(e.getDescription()))
        .findFirst()
        .orElseThrow();
    assertThat(createdExpense.getValue()).isEqualByComparingTo(BigDecimal.valueOf(100));
    assertThat(createdExpense.getCategory().getId()).isEqualTo(testCategory.getId());
    assertThat(createdExpense.getAccountId()).isEqualTo(testAccount.getId());
    
    // Verify savings decreased
    var updatedSaving = savingRepository.findByDateAndAccountId(LocalDate.now(), testAccount.getId());
    assertThat(updatedSaving).isPresent();
    assertThat(updatedSaving.get().getValue())
        .isEqualByComparingTo(initialSavingValue.subtract(BigDecimal.valueOf(100)));
  }

  @Test
  void create_shouldReturnBadRequest_whenInvalidData() throws Exception {
    var dto = new OperationRequestDto();
    dto.setDate(LocalDate.now());
    dto.setValue(BigDecimal.valueOf(-100)); // Negative value
    dto.setDescription("Test expense");
    dto.setIsPlanned(false);
    dto.setCategoryId(testCategory.getId());

    mockMvc.perform(post(apiBaseUrl + "/expenses")
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void findAllCategories_shouldReturnCategories() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/expenses/categories")
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$[0].name").value("Test Expense Category"));
  }

  @Test
  void createCategory_shouldCreateNewCategory() throws Exception {
    // Given: Verify initial state
    var initialCount = expenseCategoryRepository.count();

    var dto = new OperationCategoryRequestDto("New Expense Category");

    // When: Create category
    mockMvc.perform(post(apiBaseUrl + "/expenses/categories")
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("New Expense Category"));

    // Then: Verify database state
    assertThat(expenseCategoryRepository.count()).isEqualTo(initialCount + 1);
    var createdCategory = expenseCategoryRepository.findAll().stream()
        .filter(c -> "New Expense Category".equals(c.getName()))
        .findFirst()
        .orElseThrow();
    assertThat(createdCategory.getAccountId()).isEqualTo(testAccount.getId());
  }

  @Test
  void updateCategory_shouldUpdateExistingCategory() throws Exception {
    // Given: Verify initial state
    var initialName = testCategory.getName();
    assertThat(initialName).isEqualTo("Test Expense Category");

    var dto = new OperationCategoryRequestDto("Updated Category");

    // When: Update category
    mockMvc.perform(put(apiBaseUrl + "/expenses/categories/" + testCategory.getId())
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Category"));

    // Then: Verify database state
    var updatedCategory = expenseCategoryRepository.findById(testCategory.getId()).orElseThrow();
    assertThat(updatedCategory.getName()).isEqualTo("Updated Category");
    assertThat(updatedCategory.getAccountId()).isEqualTo(testAccount.getId());
  }

  @Test
  void deleteCategory_shouldDeleteCategory() throws Exception {
    var categoryToDelete = ExpenseCategory.builder()
        .name("Category to Delete")
        .accountId(testAccount.getId())
        .build();
    categoryToDelete = expenseCategoryRepository.save(categoryToDelete);

    // Given: Verify category exists
    var categoryId = categoryToDelete.getId();
    assertThat(expenseCategoryRepository.findById(categoryId)).isPresent();
    var initialCount = expenseCategoryRepository.count();

    // When: Delete category
    mockMvc.perform(delete(apiBaseUrl + "/expenses/categories/" + categoryId)
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk());

    // Then: Verify category is deleted from database
    assertThat(expenseCategoryRepository.findById(categoryId)).isEmpty();
    assertThat(expenseCategoryRepository.count()).isEqualTo(initialCount - 1);
  }

  @Test
  void shouldReturnUnauthorized_whenNoToken() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/expenses/categories"))
        .andExpect(status().isUnauthorized());
  }
}
