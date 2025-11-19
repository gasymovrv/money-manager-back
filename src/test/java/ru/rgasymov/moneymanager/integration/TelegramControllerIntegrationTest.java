package ru.rgasymov.moneymanager.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import ru.rgasymov.moneymanager.domain.dto.request.TelegramAuthDto;
import ru.rgasymov.moneymanager.domain.dto.request.TelegramWebhookDto;
import ru.rgasymov.moneymanager.repository.TelegramUserRepository;

/**
 * Integration tests for TelegramController.
 */
class TelegramControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private TelegramUserRepository telegramUserRepository;

  @Value("${server.api-base-url}")
  private String apiBaseUrl;

  @Test
  void linkTelegramAccount_shouldReturnUnauthorized_whenInvalidAuth() throws Exception {
    // Given: Verify telegram user doesn't exist
    var initialCount = telegramUserRepository.count();

    var authDto = new TelegramAuthDto();
    authDto.setId(123456L);
    authDto.setFirstName("Test");
    authDto.setAuthDate(System.currentTimeMillis() / 1000);
    authDto.setHash("invalid-hash");

    // When: Attempt to link with invalid hash
    mockMvc.perform(post(apiBaseUrl + "/telegram/link")
            .header("Authorization", getAuthorizationHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(authDto)))
        .andExpect(status().isUnauthorized());

    // Then: No telegram user created
    assertThat(telegramUserRepository.count()).isEqualTo(initialCount);
  }

  @Test
  void linkTelegramAccount_shouldRequireAuthentication() throws Exception {
    var authDto = new TelegramAuthDto();
    authDto.setId(123456L);
    authDto.setFirstName("Test");
    authDto.setAuthDate(System.currentTimeMillis() / 1000);
    authDto.setHash("some-hash");

    mockMvc.perform(post(apiBaseUrl + "/telegram/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(authDto)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void webhook_shouldReturnOk_whenValidRequest() throws Exception {
    // Given: Create webhook DTO
    var webhookDto = new TelegramWebhookDto();
    webhookDto.setUpdateId(12345L);
    // Empty message - should be processed without errors

    // When & Then: Process webhook
    mockMvc.perform(post(apiBaseUrl + "/telegram/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(webhookDto)))
        .andExpect(status().isOk());
    
    // Webhook processing is async and doesn't modify DB immediately for empty messages
  }

  @Test
  void webhook_shouldAcceptCallbackQuery() throws Exception {
    var webhookDto = new TelegramWebhookDto();
    webhookDto.setUpdateId(12345L);
    var callbackQuery = new TelegramWebhookDto.TelegramCallbackQueryDto();
    callbackQuery.setId("callback123");
    var from = new TelegramWebhookDto.TelegramUserDto();
    from.setId(123456L);
    callbackQuery.setFrom(from);
    webhookDto.setCallbackQuery(callbackQuery);

    mockMvc.perform(post(apiBaseUrl + "/telegram/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(webhookDto)))
        .andExpect(status().isOk());
  }

  @Test
  void webhook_shouldAcceptMessageWithoutProcessing() throws Exception {
    // Simple webhook without message - should just return OK
    var webhookDto = new TelegramWebhookDto();
    webhookDto.setUpdateId(12345L);

    mockMvc.perform(post(apiBaseUrl + "/telegram/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(webhookDto)))
        .andExpect(status().isOk());
  }
}
