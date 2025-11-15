package ru.rgasymov.moneymanager.service.telegram;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rgasymov.moneymanager.domain.dto.request.TelegramAuthDto;
import ru.rgasymov.moneymanager.domain.dto.request.TelegramWebhookDto;
import ru.rgasymov.moneymanager.domain.entity.ReportTask;
import ru.rgasymov.moneymanager.domain.entity.ReportTask.ReportTaskStatus;
import ru.rgasymov.moneymanager.domain.entity.TelegramMessage;
import ru.rgasymov.moneymanager.domain.entity.TelegramUser;
import ru.rgasymov.moneymanager.domain.entity.TelegramUserState;
import ru.rgasymov.moneymanager.domain.entity.TelegramUserState.ConversationState;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.repository.AccountRepository;
import ru.rgasymov.moneymanager.repository.ReportTaskRepository;
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

  private static final String PLEASE_ENTER_DATES =
      "Please enter the period in format START-END (date format DD.MM.YYYY).\nExample: 01.01.2024-31.12.2024";
  private final TelegramUserRepository telegramUserRepository;
  private final TelegramMessageRepository telegramMessageRepository;
  private final TelegramUserStateRepository telegramUserStateRepository;
  private final ReportTaskRepository reportTaskRepository;
  private final TelegramBotClient telegramBotClient;
  private final AccountRepository accountRepository;

  @Value("${telegram.bot.token}")
  private String botToken;

  @Value("${report.task.max-retries:3}")
  private int maxRetries;

  private static final Pattern DATE_RANGE_PATTERN =
      Pattern.compile("^(\\d{2}\\.\\d{2}\\.\\d{4})-(\\d{2}\\.\\d{2}\\.\\d{4})$");
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private static final String ACCOUNT_CB_PREFIX = "REPORT_ACC:";

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
    TelegramUser telegramUser = telegramUserRepository.findById(authDto.getId()).orElse(new TelegramUser());

    telegramUser.setTelegramId(authDto.getId());
    telegramUser.setUser(user);
    telegramUser.setFirstName(authDto.getFirstName());
    telegramUser.setLastName(authDto.getLastName());
    telegramUser.setUsername(authDto.getUsername());
    telegramUser.setPhotoUrl(authDto.getPhotoUrl());
    telegramUser.setAuthDate(LocalDateTime.ofInstant(Instant.ofEpochSecond(authDto.getAuthDate()), ZoneId.systemDefault()));

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
    // Handle callback queries (account selection)
    if (webhookDto.getCallbackQuery() != null) {
      var callback = webhookDto.getCallbackQuery();
      handleAccountSelectionCallback(callback);
      return;
    }

    // Handle plain messages
    if (webhookDto.getMessage() == null) {
      log.debug("Webhook update {} has no message/callback, skipping", webhookDto.getUpdateId());
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
      telegramBotClient.sendMessage(
          message.getChat().getId(),
          "It looks like your Telegram account isn’t linked with Money Manager yet. "
              + "Please log in to https://money-manager.ddns.net and click the Telegram button to link your account."
      );
      return;
    }

    String messageText = message.getText();
    Long chatId = message.getChat().getId();

    // Get or create user state with pessimistic lock to prevent race conditions
    TelegramUserState userState = telegramUserStateRepository.findByIdWithLock(telegramId).orElse(
        TelegramUserState.builder()
            .telegramId(telegramId)
            .state(ConversationState.NONE)
            .updatedAt(LocalDateTime.now())
            .build()
    );

    // Process message based on state
    if ("/report".equalsIgnoreCase(messageText)) {
      handleReportCommand(telegramUser, chatId, userState);
    } else if (userState.getState() == ConversationState.AWAITING_REPORT_DATES) {
      handleDateInput(telegramId, chatId, messageText, userState);
    } else {
      log.debug("Ignoring message from user {}: {}", telegramId, messageText);
      return;
    }

    // TODO Move it before last sendMessage
    telegramMessageRepository.save(TelegramMessage.builder()
        .messageId(messageId)
        .telegramId(telegramId)
        .chatId(chatId)
        .messageText(messageText)
        .processedAt(LocalDateTime.now())
        .build());
  }

  /**
   * Handle /report command.
   */
  private void handleReportCommand(TelegramUser telegramUser, Long chatId, TelegramUserState userState) {
    log.info("Processing /report command from user {}", telegramUser.getTelegramId());

    // Warn if overwriting existing conversation state
    if (userState.getState() != ConversationState.NONE) {
      log.warn("User {} started /report while in state {}, overwriting", telegramUser.getTelegramId(), userState.getState());
    }
    var accounts = accountRepository.findAllByUserId(telegramUser.getUser().getId());

    if (accounts == null || accounts.isEmpty()) {
      // No accounts
      userState.setState(ConversationState.NONE);
      userState.setSelectedAccountId(null);
      userState.setUpdatedAt(LocalDateTime.now());
      telegramUserStateRepository.save(userState);
      telegramBotClient.sendMessage(chatId, "You don't have any accounts yet. Please create an account in the app first.");
      return;
    }

    if (accounts.size() == 1) {
      // Single account - auto-select
      var acc = accounts.getFirst();
      userState.setSelectedAccountId(acc.getId());
      userState.setState(ConversationState.AWAITING_REPORT_DATES);
      userState.setUpdatedAt(LocalDateTime.now());
      telegramUserStateRepository.save(userState);
      telegramBotClient.sendMessage(chatId, PLEASE_ENTER_DATES);
      return;
    }

    // Multiple accounts - ask to select
    var rows = new ArrayList<List<TelegramBotClient.InlineKeyboardButton>>();
    for (var acc : accounts) {
      var btn = new TelegramBotClient.InlineKeyboardButton(acc.getName(), ACCOUNT_CB_PREFIX + acc.getId());
      rows.add(List.of(btn));
    }
    userState.setState(ConversationState.AWAITING_ACCOUNT_SELECTION);
    userState.setSelectedAccountId(null);
    userState.setUpdatedAt(LocalDateTime.now());
    telegramUserStateRepository.save(userState);

    telegramBotClient.sendMessageWithInlineKeyboard(chatId, "Select account for the report:", rows);
  }

  /**
   * Handle date input from user.
   */
  private void handleDateInput(
      Long telegramId,
      Long chatId,
      String messageText,
      TelegramUserState userState
  ) {
    log.info("Processing date input from user {}: {}", telegramId, messageText);

    if (userState.getSelectedAccountId() == null) {
      telegramBotClient.sendMessage(chatId, "Please select an account first. Send /report to choose an account.");
      return;
    }

    // Validate date format
    Matcher matcher = DATE_RANGE_PATTERN.matcher(messageText.trim());
    if (!matcher.matches()) {
      telegramBotClient.sendMessage(chatId, "Invalid format. Please use DD.MM.YYYY-DD.MM.YYYY format.\nExample: 01.01.2024-31.12.2024");
      return;
    }

    LocalDate startDate;
    LocalDate endDate;
    try {
      // Parse dates
      startDate = LocalDate.parse(matcher.group(1), DATE_FORMATTER);
      endDate = LocalDate.parse(matcher.group(2), DATE_FORMATTER);
    } catch (DateTimeParseException e) {
      log.warn("Failed to parse dates from user {}: {}", telegramId, messageText, e);
      telegramBotClient.sendMessage(chatId, "Error: Invalid date format. Please use DD.MM.YYYY format.");
      return;
    }

    // Validate date range
    if (startDate.isAfter(endDate)) {
      telegramBotClient.sendMessage(chatId, "Error: Start date must be before end date.");
      return;
    }
    if (ChronoUnit.DAYS.between(startDate, endDate) > 365) {
      telegramBotClient.sendMessage(chatId, "Error: Date range cannot exceed 1 year (365 days).");
      return;
    }

    ReportTask reportTask = ReportTask.builder()
        .telegramId(telegramId)
        .chatId(chatId)
        .accountId(userState.getSelectedAccountId())
        .startDate(startDate)
        .endDate(endDate)
        .status(ReportTaskStatus.PENDING)
        .retryCount(0)
        .maxRetries(maxRetries)
        .createdAt(LocalDateTime.now())
        .nextRetryAt(LocalDateTime.now())
        .build();
    reportTaskRepository.save(reportTask);

    // Reset user state
    userState.setState(ConversationState.NONE);
    userState.setSelectedAccountId(null);
    userState.setUpdatedAt(LocalDateTime.now());
    telegramUserStateRepository.save(userState);

    // Send success message
    telegramBotClient.sendMessage(
        chatId,
        """
            ✅ Your report has been queued successfully!
            
            The report will be generated and sent to you within a few minutes.
            Please be patient."""
    );

    log.info("Report task created successfully for user {}: {} to {}", telegramId, startDate, endDate);
  }

  /**
   * Handle account selection callback.
   */
  private void handleAccountSelectionCallback(TelegramWebhookDto.TelegramCallbackQueryDto callback) {
    Long telegramId = callback.getFrom().getId();
    Long chatId = callback.getMessage() != null ? callback.getMessage().getChat().getId() : null;

    TelegramUser telegramUser = telegramUserRepository.findById(telegramId).orElse(null);
    if (telegramUser == null || chatId == null) {
      return;
    }

    var data = callback.getData();
    if (data == null || !data.startsWith(ACCOUNT_CB_PREFIX)) {
      return;
    }

    Long accountId;
    try {
      accountId = Long.parseLong(data.substring(ACCOUNT_CB_PREFIX.length()));
    } catch (NumberFormatException ex) {
      log.warn("Invalid callback data from user {}: {}", telegramUser.getTelegramId(), data);
      return;
    }

    // Validate that account belongs to the user
    var accountOpt = accountRepository.findByIdAndUserId(accountId, telegramUser.getUser().getId());
    if (accountOpt.isEmpty()) {
      telegramBotClient.sendMessage(chatId, "Selected account is not available. Please try /report again.");
      return;
    }

    TelegramUserState userState = telegramUserStateRepository.findByIdWithLock(telegramId).orElse(
        TelegramUserState.builder()
            .telegramId(telegramId)
            .state(ConversationState.NONE)
            .updatedAt(LocalDateTime.now())
            .build()
    );

    userState.setSelectedAccountId(accountId);
    userState.setState(ConversationState.AWAITING_REPORT_DATES);
    userState.setUpdatedAt(LocalDateTime.now());
    telegramUserStateRepository.save(userState);

    // Acknowledge callback to stop loading animation
    telegramBotClient.answerCallbackQuery(callback.getId());

    telegramMessageRepository.save(TelegramMessage.builder()
        .messageId(callback.getMessage().getMessageId())
        .telegramId(telegramId)
        .chatId(chatId)
        .messageText(callback.getData())
        .processedAt(LocalDateTime.now())
        .build());
    telegramBotClient.sendMessage(chatId, PLEASE_ENTER_DATES);
  }
}
