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
import ru.rgasymov.moneymanager.domain.dto.response.SavingSearchResultDto;
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
  private Saving pastSaving;
  private IncomeCategory salaryCategory;
  private IncomeCategory bonusCategory;
  private ExpenseCategory foodCategory;
  private ExpenseCategory transportCategory;

  @BeforeEach
  void setUp() {
    // Create categories
    salaryCategory = incomeCategoryRepository.save(
        IncomeCategory.builder().name("Salary").accountId(testAccount.getId()).build());
    bonusCategory = incomeCategoryRepository.save(
        IncomeCategory.builder().name("Bonus").accountId(testAccount.getId()).build());
    foodCategory = expenseCategoryRepository.save(
        ExpenseCategory.builder().name("Food").accountId(testAccount.getId()).build());
    transportCategory = expenseCategoryRepository.save(
        ExpenseCategory.builder().name("Transport").accountId(testAccount.getId()).build());

    // Create past saving (10 days ago)
    pastSaving = savingRepository.save(
        Saving.builder()
            .date(LocalDate.now().minusDays(10))
            .value(BigDecimal.valueOf(1000))
            .accountId(testAccount.getId())
            .build());

    incomeRepository.save(
        Income.builder()
            .date(LocalDate.now().minusDays(10))
            .value(BigDecimal.valueOf(2000))
            .description("Previous salary")
            .isPlanned(false)
            .category(salaryCategory)
            .accountId(testAccount.getId())
            .savingId(pastSaving.getId())
            .build());

    expenseRepository.save(
        Expense.builder()
            .date(LocalDate.now().minusDays(10))
            .value(BigDecimal.valueOf(500))
            .description("Old groceries")
            .isPlanned(false)
            .category(foodCategory)
            .accountId(testAccount.getId())
            .savingId(pastSaving.getId())
            .build());

    // Create current saving (today)
    testSaving = savingRepository.save(
        Saving.builder()
            .date(LocalDate.now())
            .value(BigDecimal.valueOf(3400))
            .accountId(testAccount.getId())
            .build());

    incomeRepository.save(
        Income.builder()
            .date(LocalDate.now())
            .value(BigDecimal.valueOf(5000))
            .description("Monthly salary")
            .isPlanned(false)
            .category(salaryCategory)
            .accountId(testAccount.getId())
            .savingId(testSaving.getId())
            .build());

    incomeRepository.save(
        Income.builder()
            .date(LocalDate.now())
            .value(BigDecimal.valueOf(500))
            .description("Performance bonus")
            .isPlanned(false)
            .category(bonusCategory)
            .accountId(testAccount.getId())
            .savingId(testSaving.getId())
            .build());

    expenseRepository.save(
        Expense.builder()
            .date(LocalDate.now())
            .value(BigDecimal.valueOf(100))
            .description("Groceries")
            .isPlanned(false)
            .category(foodCategory)
            .accountId(testAccount.getId())
            .savingId(testSaving.getId())
            .build());

    expenseRepository.save(
        Expense.builder()
            .date(LocalDate.now())
            .value(BigDecimal.valueOf(50))
            .description("Bus ticket")
            .isPlanned(false)
            .category(transportCategory)
            .accountId(testAccount.getId())
            .savingId(testSaving.getId())
            .build());
  }

  @Test
  void search_shouldReturnSavingsWithDefaultCriteria() throws Exception {
    // Given: Database has 2 savings
    var dbSavings = savingRepository.findAll();
    assertThat(dbSavings).hasSize(2);

    // When: Search all savings
    var response = mockMvc.perform(get(apiBaseUrl + "/savings")
            .header("Authorization", getAuthorizationHeader())
            .param("pageNum", "0")
            .param("pageSize", "10")
            .param("sortBy", "DATE")
            .param("sortDirection", "DESC")
            .param("groupBy", "DAY"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    // Then: Parse and verify response
    var result = objectMapper.readValue(response, SavingSearchResultDto.class);
    
    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getResult()).hasSize(2);
    
    // Verify savings sorted by date DESC (today's first)
    var todaySaving = result.getResult().get(0);
    assertThat(todaySaving.getDate()).isEqualTo(LocalDate.now());
    assertThat(todaySaving.getValue()).isEqualByComparingTo(BigDecimal.valueOf(3400));
    assertThat(todaySaving.getId()).isNotNull();
    assertThat(todaySaving.getPeriod()).isNotNull();
    
    var pastSavingDto = result.getResult().get(1);
    assertThat(pastSavingDto.getDate()).isEqualTo(LocalDate.now().minusDays(10));
    assertThat(pastSavingDto.getValue()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    assertThat(pastSavingDto.getId()).isNotNull();
    
    // Verify categories are returned (categories that exist for this account)
    assertThat(result.getIncomeCategories()).hasSize(2);
    assertThat(result.getIncomeCategories()).anyMatch(c -> "Salary".equals(c.getName()));
    assertThat(result.getIncomeCategories()).anyMatch(c -> "Bonus".equals(c.getName()));
    
    assertThat(result.getExpenseCategories()).hasSize(2);
    assertThat(result.getExpenseCategories()).anyMatch(c -> "Food".equals(c.getName()));
    assertThat(result.getExpenseCategories()).anyMatch(c -> "Transport".equals(c.getName()));
  }

  @Test
  void search_shouldReturnSavingsWithDateRange() throws Exception {
    // Given: Date range that includes only today's saving
    var from = LocalDate.now().minusDays(5);
    var to = LocalDate.now();

    // When: Search with date range
    var response = mockMvc.perform(get(apiBaseUrl + "/savings")
            .header("Authorization", getAuthorizationHeader())
            .param("pageNum", "0")
            .param("pageSize", "10")
            .param("sortBy", "DATE")
            .param("sortDirection", "DESC")
            .param("groupBy", "DAY")
            .param("dateFrom", from.toString())
            .param("dateTo", to.toString()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    // Then: Verify savings are returned
    var result = objectMapper.readValue(response, SavingSearchResultDto.class);
    
    // Should return savings (may include previous savings for calculation chain)
    assertThat(result.getResult()).isNotEmpty();
    
    // Today's saving should be included
    assertThat(result.getResult()).anyMatch(s -> s.getDate().equals(LocalDate.now()));
    
    // All returned savings should not be after the end date
    assertThat(result.getResult()).allMatch(s -> !s.getDate().isAfter(to));
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
  void search_shouldFilterByIncomeCategoryOnly() throws Exception {
    // Given: Filter by bonus category (only in today's saving)
    assertThat(bonusCategory.getId()).isNotNull();

    // When: Search with bonus category filter
    var response = mockMvc.perform(get(apiBaseUrl + "/savings")
            .header("Authorization", getAuthorizationHeader())
            .param("pageNum", "0")
            .param("pageSize", "10")
            .param("sortBy", "DATE")
            .param("sortDirection", "DESC")
            .param("groupBy", "DAY")
            .param("incomeCategoryIds", bonusCategory.getId().toString()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    // Then: Savings with bonus category should be returned
    var result = objectMapper.readValue(response, SavingSearchResultDto.class);
    
    // Should filter savings that have operations in the bonus category
    assertThat(result.getResult()).isNotEmpty();
    
    // Verify Bonus category is in the returned categories list
    assertThat(result.getIncomeCategories())
        .anyMatch(c -> "Bonus".equals(c.getName()));
  }

  @Test
  void search_shouldFilterByExpenseCategoryOnly() throws Exception {
    // Given: Filter by transport category (only in today's saving)
    assertThat(transportCategory.getId()).isNotNull();

    // When: Search with transport category filter
    var response = mockMvc.perform(get(apiBaseUrl + "/savings")
            .header("Authorization", getAuthorizationHeader())
            .param("pageNum", "0")
            .param("pageSize", "10")
            .param("sortBy", "DATE")
            .param("sortDirection", "DESC")
            .param("groupBy", "DAY")
            .param("expenseCategoryIds", transportCategory.getId().toString()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    // Then: Savings with transport category should be returned
    var result = objectMapper.readValue(response, SavingSearchResultDto.class);
    
    // Should filter savings that have operations in the transport category
    assertThat(result.getResult()).isNotEmpty();
    
    // Verify Transport category is in the returned categories list
    assertThat(result.getExpenseCategories())
        .anyMatch(c -> "Transport".equals(c.getName()));
  }

  @Test
  void search_shouldFilterByMultipleCategories() throws Exception {
    // Given: Filter by Food expense (in both savings)
    assertThat(foodCategory.getId()).isNotNull();

    // When: Search with food category filter
    var response = mockMvc.perform(get(apiBaseUrl + "/savings")
            .header("Authorization", getAuthorizationHeader())
            .param("pageNum", "0")
            .param("pageSize", "10")
            .param("sortBy", "DATE")
            .param("sortDirection", "DESC")
            .param("groupBy", "DAY")
            .param("expenseCategoryIds", foodCategory.getId().toString()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    // Then: Savings with food expenses should be returned
    var result = objectMapper.readValue(response, SavingSearchResultDto.class);
    
    // Should return savings that have food expenses
    assertThat(result.getTotalElements()).isGreaterThan(0);
    assertThat(result.getResult()).isNotEmpty();
    
    // Verify Food category is in the expense categories
    assertThat(result.getExpenseCategories())
        .anyMatch(c -> "Food".equals(c.getName()));
    
    // Verify we got multiple savings (both have food)
    assertThat(result.getResult().size()).isGreaterThan(1);
  }

  @Test
  void search_shouldReturnUnauthorized_whenNoToken() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/savings"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void search_shouldFilterBySearchText() throws Exception {
    // Given: Search for "bonus" which only exists in today's saving

    // When: Search with search text filter
    var response = mockMvc.perform(get(apiBaseUrl + "/savings")
            .header("Authorization", getAuthorizationHeader())
            .param("pageNum", "0")
            .param("pageSize", "10")
            .param("sortBy", "DATE")
            .param("sortDirection", "DESC")
            .param("groupBy", "DAY")
            .param("searchText", "bonus"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    // Then: Should filter savings containing operations matching search text
    var result = objectMapper.readValue(response, SavingSearchResultDto.class);
    
    // Should return savings with operations matching "bonus"
    assertThat(result.getResult()).isNotEmpty();
    
    // Verify Bonus category appears in income categories
    assertThat(result.getIncomeCategories())
        .anyMatch(c -> "Bonus".equals(c.getName()));
  }

  @Test
  void search_shouldFilterBySearchText_matchingMultipleSavings() throws Exception {
    // Given: Search for "salary" which exists in both savings

    // When: Search with search text filter
    var response = mockMvc.perform(get(apiBaseUrl + "/savings")
            .header("Authorization", getAuthorizationHeader())
            .param("pageNum", "0")
            .param("pageSize", "10")
            .param("sortBy", "DATE")
            .param("sortDirection", "DESC")
            .param("groupBy", "DAY")
            .param("searchText", "salary"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    // Then: Both savings should appear (both have salary incomes)
    var result = objectMapper.readValue(response, SavingSearchResultDto.class);
    
    assertThat(result.getTotalElements()).isEqualTo(2);
  }

  @Test
  void search_shouldSupportPagination() throws Exception {
    // Given: 2 savings in database

    // When: Request page 0 with size 1
    var response = mockMvc.perform(get(apiBaseUrl + "/savings")
            .header("Authorization", getAuthorizationHeader())
            .param("pageNum", "0")
            .param("pageSize", "1")
            .param("sortBy", "DATE")
            .param("sortDirection", "DESC")
            .param("groupBy", "DAY"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    // Then: Should return 1 result with total 2
    var result = objectMapper.readValue(response, SavingSearchResultDto.class);
    
    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getResult()).hasSize(1);
    assertThat(result.getResult().get(0).getDate()).isEqualTo(LocalDate.now());
    
    // When: Request page 1 with size 1
    var response2 = mockMvc.perform(get(apiBaseUrl + "/savings")
            .header("Authorization", getAuthorizationHeader())
            .param("pageNum", "1")
            .param("pageSize", "1")
            .param("sortBy", "DATE")
            .param("sortDirection", "DESC")
            .param("groupBy", "DAY"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    // Then: Should return second result
    var result2 = objectMapper.readValue(response2, SavingSearchResultDto.class);
    
    assertThat(result2.getTotalElements()).isEqualTo(2);
    assertThat(result2.getResult()).hasSize(1);
    assertThat(result2.getResult().get(0).getDate()).isEqualTo(LocalDate.now().minusDays(10));
  }
}
