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
import ru.rgasymov.moneymanager.domain.entity.IncomeCategory;
import ru.rgasymov.moneymanager.domain.entity.Saving;
import ru.rgasymov.moneymanager.repository.IncomeCategoryRepository;
import ru.rgasymov.moneymanager.repository.SavingRepository;

/**
 * Integration tests for IncomeController.
 */
class IncomeControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private IncomeCategoryRepository incomeCategoryRepository;

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
    var dto = new OperationRequestDto();
    dto.setDate(LocalDate.now());
    dto.setValue(BigDecimal.valueOf(5000));
    dto.setDescription("Salary");
    dto.setIsPlanned(false);
    dto.setCategoryId(testCategory.getId());

    mockMvc.perform(post(apiBaseUrl + "/incomes")
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value(5000))
        .andExpect(jsonPath("$.description").value("Salary"));
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
    var dto = new OperationCategoryRequestDto("New Income Category");

    mockMvc.perform(post(apiBaseUrl + "/incomes/categories")
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("New Income Category"));
  }

  @Test
  void updateCategory_shouldUpdateIncomeCategory() throws Exception {
    var dto = new OperationCategoryRequestDto("Updated Category");

    mockMvc.perform(put(apiBaseUrl + "/incomes/categories/" + testCategory.getId())
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Category"));
  }

  @Test
  void deleteCategory_shouldDeleteIncomeCategory() throws Exception {
    var categoryToDelete = IncomeCategory.builder()
        .name("Category to Delete")
        .accountId(testAccount.getId())
        .build();
    categoryToDelete = incomeCategoryRepository.save(categoryToDelete);

    mockMvc.perform(delete(apiBaseUrl + "/incomes/categories/" + categoryToDelete.getId())
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk());
  }

  @Test
  void shouldReturnUnauthorized_whenNoToken() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/incomes/categories"))
        .andExpect(status().isUnauthorized());
  }
}
