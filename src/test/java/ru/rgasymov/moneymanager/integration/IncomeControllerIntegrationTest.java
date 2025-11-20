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
  void update_shouldUpdateIncomeWithSameDate() throws Exception {
    // Given: Create an income first
    var createDto = new OperationRequestDto();
    createDto.setDate(LocalDate.now());
    createDto.setValue(BigDecimal.valueOf(3000));
    createDto.setDescription("Initial Income");
    createDto.setIsPlanned(false);
    createDto.setCategoryId(testCategory.getId());

    var createResult = mockMvc.perform(post(apiBaseUrl + "/incomes")
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isOk())
        .andReturn();

    var createdIncomeJson = createResult.getResponse().getContentAsString();
    var createdId = objectMapper.readTree(createdIncomeJson).get("id").asLong();

    var initialSaving = savingRepository.findByDateAndAccountId(LocalDate.now(), testAccount.getId()).orElseThrow();
    var initialSavingValue = initialSaving.getValue();
    var initialCount = incomeRepository.count();

    // When: Update the income with same date but different value and description
    var updateDto = new OperationRequestDto();
    updateDto.setDate(LocalDate.now()); // Same date
    updateDto.setValue(BigDecimal.valueOf(4500)); // Changed value (increase of 1500)
    updateDto.setDescription("Updated Income"); // Changed description
    updateDto.setIsPlanned(false); // Keep false since date is today
    updateDto.setCategoryId(testCategory.getId());

    mockMvc.perform(put(apiBaseUrl + "/incomes/" + createdId)
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(createdId))
        .andExpect(jsonPath("$.value").value(4500))
        .andExpect(jsonPath("$.description").value("Updated Income"))
        .andExpect(jsonPath("$.isPlanned").value(false));

    // Then: Verify database state - no new income created, existing one updated
    assertThat(incomeRepository.count()).isEqualTo(initialCount);

    var updatedIncome = incomeRepository.findById(createdId).orElseThrow();
    assertThat(updatedIncome.getValue()).isEqualByComparingTo(BigDecimal.valueOf(4500));
    assertThat(updatedIncome.getDescription()).isEqualTo("Updated Income");
    assertThat(updatedIncome.getIsPlanned()).isFalse();
    assertThat(updatedIncome.getDate()).isEqualTo(LocalDate.now());

    // Verify savings increased by 1500 (difference between 4500 and 3000)
    var updatedSaving = savingRepository.findByDateAndAccountId(LocalDate.now(), testAccount.getId()).orElseThrow();
    assertThat(updatedSaving.getValue())
        .isEqualByComparingTo(initialSavingValue.add(BigDecimal.valueOf(1500)));
  }

  @Test
  void update_shouldUpdateIncomeWithDifferentDate() throws Exception {
    // Given: Create savings for both dates
    var tomorrow = LocalDate.now().plusDays(1);
    var savingTomorrow = Saving.builder()
        .date(tomorrow)
        .value(BigDecimal.valueOf(500))
        .accountId(testAccount.getId())
        .build();
    savingRepository.save(savingTomorrow);

    // Create an income for today
    var createDto = new OperationRequestDto();
    createDto.setDate(LocalDate.now());
    createDto.setValue(BigDecimal.valueOf(2000));
    createDto.setDescription("Today Income");
    createDto.setIsPlanned(false);
    createDto.setCategoryId(testCategory.getId());

    var createResult = mockMvc.perform(post(apiBaseUrl + "/incomes")
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isOk())
        .andReturn();

    var createdIncomeJson = createResult.getResponse().getContentAsString();
    var createdId = objectMapper.readTree(createdIncomeJson).get("id").asLong();

    // Capture savings AFTER creating the income
    // Important: Creating income for today affects tomorrow's saving too (recalculateOthersFunc)
    var initialTodaySaving = savingRepository.findByDateAndAccountId(LocalDate.now(), testAccount.getId()).orElseThrow();
    var initialTomorrowSaving = savingRepository.findByDateAndAccountId(tomorrow, testAccount.getId()).orElseThrow();
    var initialCount = incomeRepository.count();

    // When: Update the income to a different date
    var updateDto = new OperationRequestDto();
    updateDto.setDate(tomorrow); // Different date
    updateDto.setValue(BigDecimal.valueOf(2000)); // Same value
    updateDto.setDescription("Tomorrow Income");
    updateDto.setIsPlanned(false);
    updateDto.setCategoryId(testCategory.getId());

    mockMvc.perform(put(apiBaseUrl + "/incomes/" + createdId)
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value(2000))
        .andExpect(jsonPath("$.description").value("Tomorrow Income"));

    // Then: Verify the old income was deleted and a new one created
    // (date change triggers delete + create in BaseOperationService)
    assertThat(incomeRepository.count()).isEqualTo(initialCount);
    assertThat(incomeRepository.findById(createdId)).isEmpty(); // Old income deleted

    var newIncome = incomeRepository.findAll().stream()
        .filter(i -> "Tomorrow Income".equals(i.getDescription()))
        .findFirst()
        .orElseThrow();
    assertThat(newIncome.getId()).isNotEqualTo(createdId); // Different ID (new record)
    assertThat(newIncome.getDate()).isEqualTo(tomorrow);
    assertThat(newIncome.getValue()).isEqualByComparingTo(BigDecimal.valueOf(2000));

    // Verify today's saving - if it was the only operation, saving gets deleted
    // Otherwise, it should be decreased by 2000
    var updatedTodaySaving = savingRepository.findByDateAndAccountId(LocalDate.now(), testAccount.getId());
    if (updatedTodaySaving.isPresent()) {
      assertThat(updatedTodaySaving.get().getValue())
          .isEqualByComparingTo(initialTodaySaving.getValue().subtract(BigDecimal.valueOf(2000)));
    }
    // If empty, it means this was the only operation and the saving was deleted (valid behavior)

    // Verify tomorrow's saving - moving income from today to tomorrow has NO NET EFFECT on tomorrow:
    // 1. Deleting from today DECREASES tomorrow by 2000 (recalculateOthersFunc)
    // 2. Adding to tomorrow INCREASES tomorrow by 2000
    // Net result: tomorrow's value remains the same
    var updatedTomorrowSaving = savingRepository.findByDateAndAccountId(tomorrow, testAccount.getId());
    assertThat(updatedTomorrowSaving).isPresent();
    assertThat(updatedTomorrowSaving.get().getValue())
        .isEqualByComparingTo(initialTomorrowSaving.getValue());
  }

  @Test
  void shouldReturnUnauthorized_whenNoToken() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/incomes/categories"))
        .andExpect(status().isUnauthorized());
  }
}
