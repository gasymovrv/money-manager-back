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
import ru.rgasymov.moneymanager.domain.entity.TelegramUser;
import ru.rgasymov.moneymanager.domain.entity.TelegramUserState;
import ru.rgasymov.moneymanager.domain.entity.TelegramUserState.ConversationState;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.repository.TelegramMessageRepository;
import ru.rgasymov.moneymanager.repository.TelegramUserRepository;
import ru.rgasymov.moneymanager.repository.TelegramUserStateRepository;

/**
 * Service for handling Telegram integration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramService {

  private final TelegramUserRepository telegramUserRepository;
  private final TelegramUserStateRepository telegramUserStateRepository;
  private final TelegramMessageRepository telegramMessageRepository;
  private final TelegramBotClient telegramBotClient;
  private final TelegramCommandHandler telegramCommandHandler;

  @Value("${telegram.bot.token}")
  private String botToken;

  /**
   * Verify Telegram authentication data using bot token.
   * <br/>
   * <a href="https://core.telegram.org/widgets/login#checking-authorization">Telegram docs</a>
   *
   * @param authDto the authentication data
   * @return true if valid
   */
  public boolean verifyTelegramAuth(TelegramAuthDto authDto) {
    try {
      // Create data check string
      var dataMap = getAuthDataMap(authDto);
      var dataCheckString = new StringBuilder();

      for (var entry : dataMap.entrySet()) {
        if (!dataCheckString.isEmpty()) {
          dataCheckString.append("\n");
        }
        dataCheckString.append(entry.getKey()).append("=").append(entry.getValue());
      }

      // Calculate secret key
      var digest = MessageDigest.getInstance("SHA-256");
      var secretKey = digest.digest(botToken.getBytes(StandardCharsets.UTF_8));

      // Calculate HMAC-SHA256
      var mac = Mac.getInstance("HmacSHA256");
      var secretKeySpec = new SecretKeySpec(secretKey, "HmacSHA256");
      mac.init(secretKeySpec);
      var hmac = mac.doFinal(dataCheckString.toString().getBytes(StandardCharsets.UTF_8));

      // Convert to hex
      var hexString = new StringBuilder();
      for (byte b : hmac) {
        var hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }

      // Check if auth_date is not too old (e.g., within 1 day)
      var currentTime = System.currentTimeMillis() / 1000;
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
    var telegramUser = telegramUserRepository.findById(authDto.getId()).orElse(new TelegramUser());

    telegramUser.setTelegramId(authDto.getId());
    telegramUser.setUser(user);
    telegramUser.setFirstName(authDto.getFirstName());
    telegramUser.setLastName(authDto.getLastName());
    telegramUser.setUsername(authDto.getUsername());
    telegramUser.setPhotoUrl(authDto.getPhotoUrl());
    telegramUser.setAuthDate(LocalDateTime.ofInstant(Instant.ofEpochSecond(authDto.getAuthDate()), ZoneId.systemDefault()));

    var now = LocalDateTime.now();
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
    // Handle callback queries (account selection, category selection)
    if (webhookDto.getCallbackQuery() != null) {
      telegramCommandHandler.handleCallbackQuery(webhookDto.getCallbackQuery());
      return;
    }

    // Handle plain messages
    var message = webhookDto.getMessage();
    if (message == null) {
      log.debug("Webhook update {} has no message/callback, skipping", webhookDto.getUpdateId());
      return;
    }

    if (telegramMessageRepository.existsByMessageId(message.getMessageId())) {
      log.debug("Message {} already processed, skipping", message.getMessageId());
      return;
    }

    // Find linked user
    var telegramId = message.getFrom().getId();
    var telegramUser = telegramUserRepository.findById(telegramId);

    if (telegramUser.isEmpty()) {
      telegramBotClient.sendMessage(
          message.getChat().getId(),
          "It looks like your Telegram account isn’t linked with Money Manager yet. "
              + "Please log in to https://money-manager.ddns.net and click the Telegram button to link your account."
      );
      return;
    }

    // Get or create user state with pessimistic lock to prevent race conditions
    TelegramUserState userState = telegramUserStateRepository.findByIdWithLock(telegramId).orElse(
        TelegramUserState.builder()
            .telegramId(telegramId)
            .state(ConversationState.NONE)
            .updatedAt(LocalDateTime.now())
            .build()
    );

    // Process message based on state
    var messageText = message.getText();
    if ("/report".equalsIgnoreCase(messageText)) {
      telegramCommandHandler.handleReportCommand(telegramUser.get(), message, userState);
    } else if ("/selectAccount".equalsIgnoreCase(messageText)) {
      telegramCommandHandler.handleSelectAccountCommand(telegramUser.get(), message, userState);
    } else if ("/addExpense".equalsIgnoreCase(messageText)) {
      telegramCommandHandler.handleAddExpenseCommand(telegramUser.get(), message, userState);
    } else if ("/addIncome".equalsIgnoreCase(messageText)) {
      telegramCommandHandler.handleAddIncomeCommand(telegramUser.get(), message, userState);
    } else if (userState.getState() == ConversationState.AWAITING_REPORT_DATES) {
      telegramCommandHandler.handleReportDatesInput(telegramId, message, userState);
    } else if (userState.getState() == ConversationState.AWAITING_EXPENSE_INPUT) {
      telegramCommandHandler.handleExpenseInput(telegramUser.get(), message, userState);
    } else if (userState.getState() == ConversationState.AWAITING_INCOME_INPUT) {
      telegramCommandHandler.handleIncomeInput(telegramUser.get(), message, userState);
    } else {
      log.debug("Ignoring message from user {}: {}", telegramId, messageText);
    }
  }

  private Map<String, String> getAuthDataMap(TelegramAuthDto authDto) {
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
    return dataMap;
  }
}
