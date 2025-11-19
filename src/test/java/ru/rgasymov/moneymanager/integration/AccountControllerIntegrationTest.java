package ru.rgasymov.moneymanager.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.rgasymov.moneymanager.domain.dto.request.AccountRequestDto;
import ru.rgasymov.moneymanager.domain.entity.Account;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.repository.AccountRepository;
import ru.rgasymov.moneymanager.repository.ExpenseCategoryRepository;
import ru.rgasymov.moneymanager.repository.IncomeCategoryRepository;

/**
 * Integration tests for AccountController.
 * Tests end-to-end flow from HTTP request to database.
 */
class AccountControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private ExpenseCategoryRepository expenseCategoryRepository;

  @Autowired
  private IncomeCategoryRepository incomeCategoryRepository;

  @Value("${server.api-base-url}")
  private String apiBaseUrl;

  @Test
  void findAll_shouldReturnAllAccountsForCurrentUser() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/accounts")
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name").value("Test Account"));
  }

  @Test
  void findAll_shouldReturnUnauthorized_whenNoToken() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/accounts"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void create_shouldCreateNewAccount() throws Exception {
    // Given: Verify initial state
    var initialCount = accountRepository.count();

    var dto = new AccountRequestDto();
    dto.setName("New Account");
    dto.setTheme(AccountTheme.DARK);
    dto.setCurrency("EUR");

    // When: Create account
    mockMvc.perform(post(apiBaseUrl + "/accounts")
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("New Account"))
        .andExpect(jsonPath("$.theme").value("DARK"))
        .andExpect(jsonPath("$.currency").value("EUR"));

    // Then: Verify database state
    assertThat(accountRepository.count()).isEqualTo(initialCount + 1);
    var createdAccount = accountRepository.findAll().stream()
        .filter(a -> "New Account".equals(a.getName()))
        .findFirst()
        .orElseThrow();
    assertThat(createdAccount.getTheme()).isEqualTo(AccountTheme.DARK);
    assertThat(createdAccount.getCurrency()).isEqualTo("EUR");
    assertThat(createdAccount.getUser().getId()).isEqualTo(testUser.getId());
  }

  @Test
  void create_shouldReturnBadRequest_whenInvalidData() throws Exception {
    var dto = new AccountRequestDto();
    dto.setName("");
    dto.setTheme(AccountTheme.DARK);
    dto.setCurrency("EUR");

    mockMvc.perform(post(apiBaseUrl + "/accounts")
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void update_shouldUpdateExistingAccount() throws Exception {
    // Given: Verify initial state
    var accountId = testAccount.getId();
    var initialName = testAccount.getName();
    var initialTheme = testAccount.getTheme();
    var initialCurrency = testAccount.getCurrency();
    assertThat(initialName).isEqualTo("Test Account");
    assertThat(initialTheme).isEqualTo(AccountTheme.LIGHT);
    assertThat(initialCurrency).isEqualTo("USD");

    var dto = new AccountRequestDto();
    dto.setName("Updated Account");
    dto.setTheme(AccountTheme.DARK);
    dto.setCurrency("EUR");

    // When: Update account
    mockMvc.perform(put(apiBaseUrl + "/accounts/" + accountId)
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Account"))
        .andExpect(jsonPath("$.theme").value("DARK"))
        .andExpect(jsonPath("$.currency").value("EUR"));

    // Then: Verify database state
    var updatedAccount = accountRepository.findById(accountId).orElseThrow();
    assertThat(updatedAccount.getName()).isEqualTo("Updated Account");
    assertThat(updatedAccount.getTheme()).isEqualTo(AccountTheme.DARK);
    assertThat(updatedAccount.getCurrency()).isEqualTo("EUR");
  }

  @Test
  void update_shouldReturnNotFound_whenAccountDoesNotExist() throws Exception {
    var dto = new AccountRequestDto();
    dto.setName("Updated Account");
    dto.setTheme(AccountTheme.DARK);
    dto.setCurrency("EUR");

    mockMvc.perform(put(apiBaseUrl + "/accounts/99999")
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isNotFound());
  }

  @Test
  void delete_shouldDeleteAccount_whenNotCurrentAccount() throws Exception {
    // Create a second account
    var secondAccount = Account.builder()
        .name("Second Account")
        .theme(AccountTheme.LIGHT)
        .currency("USD")
        .user(testUser)
        .build();
    secondAccount = accountRepository.save(secondAccount);

    // Given: Verify account exists
    var accountId = secondAccount.getId();
    assertThat(accountRepository.findById(accountId)).isPresent();
    var initialCount = accountRepository.count();

    // When: Delete account
    mockMvc.perform(delete(apiBaseUrl + "/accounts/" + accountId)
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk());

    // Then: Verify account is deleted from database
    assertThat(accountRepository.findById(accountId)).isEmpty();
    assertThat(accountRepository.count()).isEqualTo(initialCount - 1);
  }

  @Test
  void delete_shouldReturnBadRequest_whenDeletingCurrentAccount() throws Exception {
    mockMvc.perform(delete(apiBaseUrl + "/accounts/" + testAccount.getId())
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void changeCurrent_shouldChangeCurrentAccount() throws Exception {
    // Create a second account
    var secondAccount = Account.builder()
        .name("Second Account")
        .theme(AccountTheme.DARK)
        .currency("EUR")
        .user(testUser)
        .build();
    secondAccount = accountRepository.save(secondAccount);

    // Given: Verify initial current account
    var initialCurrentAccountId = testUser.getCurrentAccount().getId();
    assertThat(initialCurrentAccountId).isEqualTo(testAccount.getId());

    // When: Change current account
    mockMvc.perform(post(apiBaseUrl + "/accounts/change")
            .header("Authorization", getAuthorizationHeader())
            .param("id", secondAccount.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Second Account"));

    // Then: Verify current account changed in database
    var updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
    assertThat(updatedUser.getCurrentAccount().getId()).isEqualTo(secondAccount.getId());
    assertThat(updatedUser.getCurrentAccount().getName()).isEqualTo("Second Account");
  }

  @Test
  void getAllCurrencies_shouldReturnListOfCurrencies() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/accounts/currencies")
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  void createDefaultCategories_shouldCreateDefaultCategories() throws Exception {
    // Given: Verify initial category counts
    var initialIncomeCount = incomeCategoryRepository.count();
    var initialExpenseCount = expenseCategoryRepository.count();

    // When: Create default categories
    mockMvc.perform(post(apiBaseUrl + "/accounts/default-categories")
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk());

    // Then: Verify categories were created in database
    // Default categories: 2 income categories (Salary, Other Income)
    // and 5 expense categories (Food, Transport, Health, Entertainment, Other Expenses)
    assertThat(incomeCategoryRepository.count()).isEqualTo(initialIncomeCount + 2);
    assertThat(expenseCategoryRepository.count()).isEqualTo(initialExpenseCount + 5);
  }
}
