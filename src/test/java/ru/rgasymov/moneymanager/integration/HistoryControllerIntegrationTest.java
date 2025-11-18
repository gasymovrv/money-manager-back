package ru.rgasymov.moneymanager.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/**
 * Integration tests for HistoryController.
 */
class HistoryControllerIntegrationTest extends BaseIntegrationTest {

  @Value("${server.api-base-url}")
  private String apiBaseUrl;

  @Test
  void findAll_shouldReturnPagedHistoryActions() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/history")
            .header("Authorization", getAuthorizationHeader())
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  void findAll_shouldReturnUnauthorized_whenNoToken() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/history"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void findAll_shouldSupportPagination() throws Exception {
    mockMvc.perform(get(apiBaseUrl + "/history")
            .header("Authorization", getAuthorizationHeader())
            .param("page", "0")
            .param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.size").value(5));
  }
}
