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
import ru.rgasymov.moneymanager.domain.entity.IncomeCategory;
import ru.rgasymov.moneymanager.domain.entity.Saving;
import ru.rgasymov.moneymanager.repository.IncomeCategoryRepository;
import ru.rgasymov.moneymanager.repository.IncomeRepository;
import ru.rgasymov.moneymanager.repository.SavingRepository;

/**
 * Integration tests for IncomeController.
 */
class IncomeControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private IncomeCategoryRepository incomeCategoryRepository;

  @Autowired
  private IncomeRepository incomeRepository;

  @Autowired
  private SavingRepository savingRepository;

  @Value("${server.api-base-url}")
  private String apiBaseUrl;

  private IncomeCategory testCategory;

  @BeforeEach
  void setUp() {
    testCategory = IncomeCategory.builder()
        .name("Test Income Category")
        .accountId(testAccount.getId())
        .build();
    testCategory = incomeCategoryRepository.save(testCategory);

    var saving = Saving.builder()
        .date(LocalDate.now())
        .value(BigDecimal.valueOf(0))
        .accountId(testAccount.getId())
        .build();
    savingRepository.save(saving);
  }

  @Test
  void create_shouldCreateNewIncome() throws Exception {
    // Given: Verify initial state
    var initialCount = incomeRepository.count();
    var initialSaving = savingRepository.findByDateAndAccountId(LocalDate.now(), testAccount.getId());
    var initialSavingValue = initialSaving.map(Saving::getValue).orElse(BigDecimal.ZERO);

    var dto = new OperationRequestDto();
    dto.setDate(LocalDate.now());
    dto.setValue(BigDecimal.valueOf(5000));
    dto.setDescription("Salary");
    dto.setIsPlanned(false);
    dto.setCategoryId(testCategory.getId());

    // When: Create income
    mockMvc.perform(post(apiBaseUrl + "/incomes")
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value(5000))
        .andExpect(jsonPath("$.description").value("Salary"));

    // Then: Verify database state after creation
    assertThat(incomeRepository.count()).isEqualTo(initialCount + 1);
    
    var createdIncome = incomeRepository.findAll().stream()
        .filter(i -> "Salary".equals(i.getDescription()))
        .findFirst()
        .orElseThrow();
    assertThat(createdIncome.getValue()).isEqualByComparingTo(BigDecimal.valueOf(5000));
    assertThat(createdIncome.getCategory().getId()).isEqualTo(testCategory.getId());
    assertThat(createdIncome.getAccountId()).isEqualTo(testAccount.getId());
    
    // Verify savings increased
    var updatedSaving = savingRepository.findByDateAndAccountId(LocalDate.now(), testAccount.getId());
    assertThat(updatedSaving).isPresent();
    assertThat(updatedSaving.get().getValue())
        .isEqualByComparingTo(initialSavingValue.add(BigDecimal.valueOf(5000)));
  }

  @Test
  void findAllCategories_shouldReturnIncomeCategories() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/incomes/categories")
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$[0].name").value("Test Income Category"));
  }

  @Test
  void createCategory_shouldCreateNewIncomeCategory() throws Exception {
    // Given: Verify initial state
    var initialCount = incomeCategoryRepository.count();

    var dto = new OperationCategoryRequestDto("New Income Category");

    // When: Create category
    mockMvc.perform(post(apiBaseUrl + "/incomes/categories")
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("New Income Category"));

    // Then: Verify database state
    assertThat(incomeCategoryRepository.count()).isEqualTo(initialCount + 1);
    var createdCategory = incomeCategoryRepository.findAll().stream()
        .filter(c -> "New Income Category".equals(c.getName()))
        .findFirst()
        .orElseThrow();
    assertThat(createdCategory.getAccountId()).isEqualTo(testAccount.getId());
  }

  @Test
  void updateCategory_shouldUpdateIncomeCategory() throws Exception {
    // Given: Verify initial state
    var initialName = testCategory.getName();
    assertThat(initialName).isEqualTo("Test Income Category");

    var dto = new OperationCategoryRequestDto("Updated Category");

    // When: Update category
    mockMvc.perform(put(apiBaseUrl + "/incomes/categories/" + testCategory.getId())
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Category"));

    // Then: Verify database state
    var updatedCategory = incomeCategoryRepository.findById(testCategory.getId()).orElseThrow();
    assertThat(updatedCategory.getName()).isEqualTo("Updated Category");
    assertThat(updatedCategory.getAccountId()).isEqualTo(testAccount.getId());
  }

  @Test
  void deleteCategory_shouldDeleteIncomeCategory() throws Exception {
    var categoryToDelete = IncomeCategory.builder()
        .name("Category to Delete")
        .accountId(testAccount.getId())
        .build();
    categoryToDelete = incomeCategoryRepository.save(categoryToDelete);

    // Given: Verify category exists
    var categoryId = categoryToDelete.getId();
    assertThat(incomeCategoryRepository.findById(categoryId)).isPresent();
    var initialCount = incomeCategoryRepository.count();

    // When: Delete category
    mockMvc.perform(delete(apiBaseUrl + "/incomes/categories/" + categoryId)
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk());

    // Then: Verify category is deleted from database
    assertThat(incomeCategoryRepository.findById(categoryId)).isEmpty();
    assertThat(incomeCategoryRepository.count()).isEqualTo(initialCount - 1);
  }

  @Test
  void shouldReturnUnauthorized_whenNoToken() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/incomes/categories"))
        .andExpect(status().isUnauthorized());
  }
}
