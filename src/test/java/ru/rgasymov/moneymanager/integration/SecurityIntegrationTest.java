package ru.rgasymov.moneymanager.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/**
 * Integration tests for security and authentication.
 */
class SecurityIntegrationTest extends BaseIntegrationTest {

  @Value("${server.api-base-url}")
  private String apiBaseUrl;

  @Test
  void shouldAllowAccess_whenValidToken() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/users/current")
            .header("Authorization", getAuthorizationHeader()))
        .andExpect(status().isOk());
  }

  @Test
  void shouldDenyAccess_whenNoToken() throws Exception {
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
