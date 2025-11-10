package ru.rgasymov.moneymanager.service.telegram;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rgasymov.moneymanager.domain.dto.request.TelegramAuthDto;
import ru.rgasymov.moneymanager.domain.dto.request.TelegramWebhookDto;
import ru.rgasymov.moneymanager.domain.entity.TelegramMessage;
import ru.rgasymov.moneymanager.domain.entity.TelegramUser;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.repository.TelegramMessageRepository;
import ru.rgasymov.moneymanager.repository.TelegramUserRepository;

/**
 * Service for handling Telegram integration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramService {

  private final TelegramUserRepository telegramUserRepository;
  private final TelegramMessageRepository telegramMessageRepository;

  @Value("${telegram.bot.token}")
  private String botToken;

  /**
   * Verify Telegram authentication data.
   *
   * @param authDto the authentication data
   * @return true if valid
   */
  public boolean  verifyTelegramAuth(TelegramAuthDto authDto) {
    try {
      // Create data check string
      Map<String, String> dataMap = new TreeMap<>();
      dataMap.put("auth_date", String.valueOf(authDto.getAuthDate()));
      if (authDto.getFirstName() != null) {
        dataMap.put("first_name", authDto.getFirstName());
      }
      dataMap.put("id", String.valueOf(authDto.getId()));
      if (authDto.getLastName() != null) {
        dataMap.put("last_name", authDto.getLastName());
      }
      if (authDto.getPhotoUrl() != null) {
        dataMap.put("photo_url", authDto.getPhotoUrl());
      }
      if (authDto.getUsername() != null) {
        dataMap.put("username", authDto.getUsername());
      }

      StringBuilder dataCheckString = new StringBuilder();
      for (Map.Entry<String, String> entry : dataMap.entrySet()) {
        if (dataCheckString.length() > 0) {
          dataCheckString.append("\n");
        }
        dataCheckString.append(entry.getKey()).append("=").append(entry.getValue());
      }

      // Calculate secret key
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] secretKey = digest.digest(botToken.getBytes(StandardCharsets.UTF_8));

      // Calculate HMAC-SHA256
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "HmacSHA256");
      mac.init(secretKeySpec);
      byte[] hmac = mac.doFinal(dataCheckString.toString().getBytes(StandardCharsets.UTF_8));

      // Convert to hex
      StringBuilder hexString = new StringBuilder();
      for (byte b : hmac) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }

      // Check if auth_date is not too old (e.g., within 1 day)
      long currentTime = System.currentTimeMillis() / 1000;
      if (currentTime - authDto.getAuthDate() > 86400) {
        log.warn("Telegram auth data is too old");
        return false;
      }

      return hexString.toString().equals(authDto.getHash());
    } catch (Exception e) {
      log.error("Error verifying Telegram auth", e);
      return false;
    }
  }

  /**
   * Link Telegram account to Money Manager user.
   *
   * @param authDto the authentication data
   * @param user    the Money Manager user
   */
  @Transactional
  public void linkTelegramUser(TelegramAuthDto authDto, User user) {
    TelegramUser telegramUser = telegramUserRepository.findById(authDto.getId())
        .orElse(new TelegramUser());

    telegramUser.setTelegramId(authDto.getId());
    telegramUser.setUser(user);
    telegramUser.setFirstName(authDto.getFirstName());
    telegramUser.setLastName(authDto.getLastName());
    telegramUser.setUsername(authDto.getUsername());
    telegramUser.setPhotoUrl(authDto.getPhotoUrl());
    telegramUser.setAuthDate(LocalDateTime.ofInstant(
        Instant.ofEpochSecond(authDto.getAuthDate()),
        ZoneId.systemDefault()
    ));

    LocalDateTime now = LocalDateTime.now();
    if (telegramUser.getCreatedAt() == null) {
      telegramUser.setCreatedAt(now);
    }
    telegramUser.setUpdatedAt(now);

    telegramUserRepository.save(telegramUser);
    log.info("Linked Telegram user {} to Money Manager user {}", authDto.getId(), user.getId());
  }

  /**
   * Process webhook update from Telegram.
   *
   * @param webhookDto the webhook data
   */
  @Transactional
  public void processWebhook(TelegramWebhookDto webhookDto) {
    if (webhookDto.getMessage() == null) {
      log.debug("Webhook update {} has no message, skipping", webhookDto.getUpdateId());
      return;
    }

    TelegramWebhookDto.TelegramMessageDto message = webhookDto.getMessage();
    Long messageId = message.getMessageId();

    // Check for duplicate
    if (telegramMessageRepository.existsByMessageId(messageId)) {
      log.debug("Message {} already processed, skipping", messageId);
      return;
    }

    // Find linked user
    Long telegramId = message.getFrom().getId();
    TelegramUser telegramUser = telegramUserRepository.findById(telegramId)
        .orElse(null);

    if (telegramUser == null) {
      log.warn("Telegram user {} not linked to Money Manager, ignoring message", telegramId);
      return;
    }

    // Save message to prevent duplicate processing
    TelegramMessage telegramMessage = TelegramMessage.builder()
        .messageId(messageId)
        .telegramId(telegramId)
        .chatId(message.getChat().getId())
        .messageText(message.getText())
        .processedAt(LocalDateTime.now())
        .build();
    telegramMessageRepository.save(telegramMessage);

    // Log the message for now
    log.info("Received message from Telegram user {}: {}", 
        telegramUser.getUser().getName(), message.getText());
    log.info("Message details - ID: {}, Chat ID: {}, From: {} {}", 
        messageId, message.getChat().getId(), 
        message.getFrom().getFirstName(), message.getFrom().getLastName());
  }

  /**
   * Find Telegram user by Money Manager user ID.
   *
   * @param userId the user ID
   * @return TelegramUser or null
   */
  public TelegramUser findByUserId(String userId) {
    return telegramUserRepository.findByUserId(userId).orElse(null);
  }
}
