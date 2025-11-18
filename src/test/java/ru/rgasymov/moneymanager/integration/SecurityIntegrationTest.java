package ru.rgasymov.moneymanager.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.rgasymov.moneymanager.repository.UserRepository;

/**
 * Integration tests for security and authentication.
 */
class SecurityIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private UserRepository userRepository;

  @Value("${server.api-base-url}")
  private String apiBaseUrl;

  @Test
  void shouldAllowAccess_whenValidToken() throws Exception {
    // Given: Verify user exists in database
    var user = userRepository.findById(testUser.getId());
    assertThat(user).isPresent();

    // When & Then: Valid token grants access
    mockMvc.perform(get(apiBaseUrl + "/users/current")
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk());
  }

  @Test
  void shouldDenyAccess_whenNoToken() throws Exception {
    // Given: User exists but no authentication
    assertThat(userRepository.findById(testUser.getId())).isPresent();

    // When & Then: No token denies access
    mockMvc.perform(get(apiBaseUrl + "/users/current"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldDenyAccess_whenInvalidToken() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/users/current")
            .header("Authorization", "Bearer invalid-token-here"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldDenyAccess_whenMalformedAuthorizationHeader() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/users/current")
            .header("Authorization", "InvalidFormat " + authToken))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldDenyAccess_whenTokenWithoutBearerPrefix() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/users/current")
            .header("Authorization", authToken))
        .andExpect(status().isUnauthorized());
  }
}
