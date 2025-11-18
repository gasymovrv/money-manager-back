package ru.rgasymov.moneymanager.integration;

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
import ru.rgasymov.moneymanager.repository.SavingRepository;

/**
 * Integration tests for ExpenseController.
 * Tests end-to-end flow from HTTP request to database.
 */
class ExpenseControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private ExpenseCategoryRepository expenseCategoryRepository;

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
    var dto = new OperationRequestDto();
    dto.setDate(LocalDate.now());
    dto.setValue(BigDecimal.valueOf(100));
    dto.setDescription("Test expense");
    dto.setIsPlanned(false);
    dto.setCategoryId(testCategory.getId());

    mockMvc.perform(post(apiBaseUrl + "/expenses")
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value(100))
        .andExpect(jsonPath("$.description").value("Test expense"));
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
    var dto = new OperationCategoryRequestDto("New Expense Category");

    mockMvc.perform(post(apiBaseUrl + "/expenses/categories")
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("New Expense Category"));
  }

  @Test
  void updateCategory_shouldUpdateExistingCategory() throws Exception {
    var dto = new OperationCategoryRequestDto("Updated Category");

    mockMvc.perform(put(apiBaseUrl + "/expenses/categories/" + testCategory.getId())
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Category"));
  }

  @Test
  void deleteCategory_shouldDeleteCategory() throws Exception {
    var categoryToDelete = ExpenseCategory.builder()
        .name("Category to Delete")
        .accountId(testAccount.getId())
        .build();
    categoryToDelete = expenseCategoryRepository.save(categoryToDelete);

    mockMvc.perform(delete(apiBaseUrl + "/expenses/categories/" + categoryToDelete.getId())
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk());
  }

  @Test
  void shouldReturnUnauthorized_whenNoToken() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/expenses/categories"))
        .andExpect(status().isUnauthorized());
  }
}
