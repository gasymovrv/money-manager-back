package ru.rgasymov.moneymanager.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.rgasymov.moneymanager.repository.UserRepository;

/**
 * Integration tests for UserController.
 * Tests end-to-end flow from HTTP request to database.
 */
class UserControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private UserRepository userRepository;

  @Value("${server.api-base-url}")
  private String apiBaseUrl;

  @Test
  void getCurrentUser_shouldReturnCurrentUserDetails() throws Exception {
    // Given: Verify user exists in database
    var user = userRepository.findById(testUser.getId()).orElseThrow();
    assertThat(user.getEmail()).isEqualTo("test@example.com");
    assertThat(user.getName()).isEqualTo("Test User");

    // When: Get current user
    mockMvc.perform(get(apiBaseUrl + "/users/current")
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testUser.getId()))
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.name").value("Test User"));

    // Then: Response matches database state
    assertThat(user.getId()).isEqualTo(testUser.getId());
  }

  @Test
  void getCurrentUser_shouldReturnUnauthorized_whenNoToken() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/users/current"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getCurrentUser_shouldIncludeCurrentAccount() throws Exception {
    // Given: Verify user has current account in database
    var user = userRepository.findById(testUser.getId()).orElseThrow();
    assertThat(user.getCurrentAccount()).isNotNull();
    assertThat(user.getCurrentAccount().getName()).isEqualTo("Test Account");

    // When & Then: Response includes current account from database
    mockMvc.perform(get(apiBaseUrl + "/users/current")
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentAccount").exists())
        .andExpect(jsonPath("$.currentAccount.name").value("Test Account"));
  }
}
