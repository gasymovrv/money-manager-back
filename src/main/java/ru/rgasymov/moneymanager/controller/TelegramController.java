package ru.rgasymov.moneymanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rgasymov.moneymanager.domain.dto.request.TelegramAuthDto;
import ru.rgasymov.moneymanager.domain.dto.request.TelegramWebhookDto;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.service.UserService;
import ru.rgasymov.moneymanager.service.telegram.TelegramService;

/**
 * Controller for Telegram integration endpoints.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("${server.api-base-url}/telegram")
@Slf4j
public class TelegramController {

  private final TelegramService telegramService;

  @Value("${telegram.bot.webhook-secret:}")
  private String webhookSecret;
  private final UserService userService;

  /**
   * Link Telegram account to current user.
   *
   * @param authDto the Telegram authentication data
   * @return response entity
   */
  @Operation(summary = "Link Telegram account to current user")
  @SecurityRequirement(name = "bearerAuth")
  @PostMapping("/link")
  public ResponseEntity<String> linkTelegramAccount(@Valid @RequestBody TelegramAuthDto authDto) {
    log.info("Received Telegram link request for telegram ID: {}", authDto.getId());

    if (!telegramService.verifyTelegramAuth(authDto)) {
      log.warn("Invalid Telegram authentication data");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid authentication data");
    }

    User currentUser = userService.getCurrentUser();
    telegramService.linkTelegramUser(authDto, currentUser);

    return ResponseEntity.ok("Telegram account linked successfully");
  }

  /**
   * Webhook endpoint for receiving updates from Telegram bot.
   *
   * @param secretToken the secret token from Telegram header
   * @param webhookDto  the webhook update
   * @return response entity
   */
  @Operation(summary = "Telegram webhook endpoint")
  @PostMapping("/webhook")
  public ResponseEntity<Void> webhook(
      @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken,
      @RequestBody TelegramWebhookDto webhookDto
  ) {

    // Verify secret token if configured
    if (webhookSecret != null && !webhookSecret.isEmpty()) {
      if (secretToken == null || !secretToken.equals(webhookSecret)) {
        log.warn("Invalid or missing webhook secret token");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }
    }

    log.debug("Received Telegram webhook update: {}", webhookDto.getUpdateId());
    telegramService.processWebhook(webhookDto);
    return ResponseEntity.ok().build();
  }
}
