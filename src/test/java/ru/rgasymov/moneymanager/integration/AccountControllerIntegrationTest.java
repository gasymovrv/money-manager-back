package ru.rgasymov.moneymanager.integration;

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

/**
 * Integration tests for AccountController.
 * Tests end-to-end flow from HTTP request to database.
 */
class AccountControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private AccountRepository accountRepository;

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
    var dto = new AccountRequestDto();
    dto.setName("New Account");
    dto.setTheme(AccountTheme.DARK);
    dto.setCurrency("EUR");

    mockMvc.perform(post(apiBaseUrl + "/accounts")
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("New Account"))
        .andExpect(jsonPath("$.theme").value("DARK"))
        .andExpect(jsonPath("$.currency").value("EUR"));
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
    var accountId = testAccount.getId();
    var dto = new AccountRequestDto();
    dto.setName("Updated Account");
    dto.setTheme(AccountTheme.DARK);
    dto.setCurrency("EUR");

    mockMvc.perform(put(apiBaseUrl + "/accounts/" + accountId)
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Account"))
        .andExpect(jsonPath("$.theme").value("DARK"))
        .andExpect(jsonPath("$.currency").value("EUR"));
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

    mockMvc.perform(delete(apiBaseUrl + "/accounts/" + secondAccount.getId())
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk());
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

    mockMvc.perform(post(apiBaseUrl + "/accounts/change")
            .header("Authorization", getAuthorizationHeader())
            .param("id", secondAccount.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Second Account"));
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
    mockMvc.perform(post(apiBaseUrl + "/accounts/default-categories")
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk());
  }
}
