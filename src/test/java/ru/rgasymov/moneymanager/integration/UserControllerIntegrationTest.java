package ru.rgasymov.moneymanager.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/**
 * Integration tests for UserController.
 * Tests end-to-end flow from HTTP request to database.
 */
class UserControllerIntegrationTest extends BaseIntegrationTest {

  @Value("${server.api-base-url}")
  private String apiBaseUrl;

  @Test
  void getCurrentUser_shouldReturnCurrentUserDetails() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/users/current")
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testUser.getId()))
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.name").value("Test User"));
  }

  @Test
  void getCurrentUser_shouldReturnUnauthorized_whenNoToken() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/users/current"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getCurrentUser_shouldIncludeCurrentAccount() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/users/current")
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentAccount").exists())
        .andExpect(jsonPath("$.currentAccount.name").value("Test Account"));
  }
}
