package ru.rgasymov.moneymanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rgasymov.moneymanager.domain.dto.request.TelegramAuthDto;
import ru.rgasymov.moneymanager.domain.dto.request.TelegramWebhookDto;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.service.UserService;
import ru.rgasymov.moneymanager.service.telegram.TelegramService;

/**
 * Controller for Telegram integration.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("${server.api-base-url}/telegram")
@Slf4j
public class TelegramController {

  private final TelegramService telegramService;
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
   * Webhook endpoint for Telegram bot.
   *
   * @param webhookDto the webhook data
   * @return response entity
   */
  @Operation(summary = "Telegram webhook endpoint")
  @PostMapping("/webhook")
  public ResponseEntity<String> webhook(@RequestBody TelegramWebhookDto webhookDto) {
    log.debug("Received Telegram webhook update: {}", webhookDto.getUpdateId());

    try {
      telegramService.processWebhook(webhookDto);
      return ResponseEntity.ok("OK");
    } catch (Exception e) {
      log.error("Error processing Telegram webhook", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
    }
  }
}
