package ru.rgasymov.moneymanager.service.telegram;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.rgasymov.moneymanager.domain.dto.request.OperationRequestDto;
import ru.rgasymov.moneymanager.domain.dto.request.TelegramWebhookDto;
import ru.rgasymov.moneymanager.domain.entity.BaseOperation;
import ru.rgasymov.moneymanager.domain.entity.BaseOperationCategory;
import ru.rgasymov.moneymanager.domain.entity.ReportTask;
import ru.rgasymov.moneymanager.domain.entity.ReportTask.ReportTaskStatus;
import ru.rgasymov.moneymanager.domain.entity.TelegramMessage;
import ru.rgasymov.moneymanager.domain.entity.TelegramUser;
import ru.rgasymov.moneymanager.domain.entity.TelegramUserState;
import ru.rgasymov.moneymanager.domain.entity.TelegramUserState.ConversationState;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.repository.AccountRepository;
import ru.rgasymov.moneymanager.repository.ExpenseCategoryRepository;
import ru.rgasymov.moneymanager.repository.IncomeCategoryRepository;
import ru.rgasymov.moneymanager.repository.ReportTaskRepository;
import ru.rgasymov.moneymanager.repository.TelegramMessageRepository;
import ru.rgasymov.moneymanager.repository.TelegramUserRepository;
import ru.rgasymov.moneymanager.repository.TelegramUserStateRepository;
import ru.rgasymov.moneymanager.security.UserPrincipal;
import ru.rgasymov.moneymanager.service.BaseOperationService;
import ru.rgasymov.moneymanager.service.expense.ExpenseService;
import ru.rgasymov.moneymanager.service.income.IncomeService;
import ru.rgasymov.moneymanager.spec.ExpenseCategorySpec;
import ru.rgasymov.moneymanager.spec.IncomeCategorySpec;

/**
 * Service for handling Telegram integration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramCommandHandler {
  private final TelegramUserRepository telegramUserRepository;
  private final TelegramMessageRepository telegramMessageRepository;
  private final TelegramUserStateRepository telegramUserStateRepository;
  private final ReportTaskRepository reportTaskRepository;
  private final TelegramBotClient telegramBotClient;
  private final AccountRepository accountRepository;
  private final ExpenseCategoryRepository expenseCategoryRepository;
  private final IncomeCategoryRepository incomeCategoryRepository;
  private final ExpenseService expenseService;
  private final IncomeService incomeService;

  @Value("${report.task.max-retries:3}")
  private int maxRetries;

  private static final String SELECT_ACCOUNT_FIRST = "Please select an account first using /selectAccount command.";
  private static final Pattern DATE_RANGE_PATTERN =
      Pattern.compile("^(\\d{2}\\.\\d{2}\\.\\d{4})-(\\d{2}\\.\\d{2}\\.\\d{4})$");
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private static final String SELECT_ACCOUNT_CB_PREFIX = "SELECT_ACC:";
  private static final String EXPENSE_CATEGORY_CB_PREFIX = "EXP_CAT:";
  private static final String INCOME_CATEGORY_CB_PREFIX = "INC_CAT:";

  /**
   * Handle /selectAccount command.
   * Inline keyboard with accounts is sent to the user (callback query starts).
   */
  public void handleSelectAccountCommand(
      TelegramUser telegramUser,
      TelegramWebhookDto.TelegramMessageDto message,
      TelegramUserState userState
  ) {
    var telegramId = telegramUser.getTelegramId();
    var chatId = message.getChat().getId();
    log.info("Processing /selectAccount command from user {}", telegramId);
    saveTelegramMessage(message.getMessageId(), telegramId, chatId, message.getText());

    var accounts = accountRepository.findAllByUserId(telegramUser.getUser().getId());
    if (accounts == null || accounts.isEmpty()) {
      saveUserState(userState, ConversationState.NONE, null);
      telegramBotClient.sendMessage(chatId, "You don't have any accounts yet. Please create an account in the app first.");
      return;
    }

    if (accounts.size() == 1) {
      var acc = accounts.getFirst();
      saveUserState(userState, ConversationState.NONE, acc.getId());
      telegramBotClient.sendMessage(chatId, "Account '" + acc.getName() + "' has been selected.");
      return;
    }

    var rows = new ArrayList<List<TelegramBotClient.InlineKeyboardButton>>();
    for (var acc : accounts) {
      var btn = new TelegramBotClient.InlineKeyboardButton(acc.getName(), SELECT_ACCOUNT_CB_PREFIX + acc.getId());
      rows.add(List.of(btn));
    }
    saveUserState(userState, ConversationState.AWAITING_ACCOUNT_SELECTION, null);
    telegramBotClient.sendMessageWithInlineKeyboard(chatId, "Select your account:", rows);
  }

  /**
   * Handle /report command.
   * Requires account to be selected via /selectAccount first.
   * This command leads to AWAITING_REPORT_DATES.
   * Instructions to enter dates are sent to the user.
   */
  public void handleReportCommand(
      TelegramUser telegramUser,
      TelegramWebhookDto.TelegramMessageDto message,
      TelegramUserState userState
  ) {
    var telegramId = telegramUser.getTelegramId();
    var chatId = message.getChat().getId();
    log.info("Processing /report command from user {}", telegramId);
    saveTelegramMessage(message.getMessageId(), telegramId, chatId, message.getText());

    if (userState.getSelectedAccountId() == null) {
      telegramBotClient.sendMessage(chatId, SELECT_ACCOUNT_FIRST);
      return;
    }

    saveUserState(userState, ConversationState.AWAITING_REPORT_DATES, userState.getSelectedAccountId());
    telegramBotClient.sendMessage(chatId, "Please enter the period in format START-END (date format DD.MM.YYYY).\nExample: 01.01.2024-31.12.2024");
  }

  /**
   * Handle /addExpense command.
   * Requires account to be selected via /selectAccount first.
   * This command leads to AWAITING_EXPENSE_CATEGORY_SELECTION.
   * Inline keyboard with expense categories is sent to the user (callback query starts).
   */
  public void handleAddExpenseCommand(
      TelegramUser telegramUser,
      TelegramWebhookDto.TelegramMessageDto message,
      TelegramUserState userState
  ) {
    var telegramId = telegramUser.getTelegramId();
    var chatId = message.getChat().getId();
    log.info("Processing /addExpense command from user {}", telegramId);
    saveTelegramMessage(message.getMessageId(), telegramId, chatId, message.getText());

    if (userState.getSelectedAccountId() == null) {
      telegramBotClient.sendMessage(chatId, SELECT_ACCOUNT_FIRST);
      return;
    }

    var categories = expenseCategoryRepository.findAll(
        ExpenseCategorySpec.accountIdEq(userState.getSelectedAccountId())
    );

    if (categories.isEmpty()) {
      saveUserState(userState, ConversationState.NONE, userState.getSelectedAccountId());
      telegramBotClient.sendMessage(chatId, "You don't have any expense categories yet. Please create one in the app first.");
      return;
    }

    var rows = new ArrayList<List<TelegramBotClient.InlineKeyboardButton>>();
    for (var cat : categories) {
      var btn = new TelegramBotClient.InlineKeyboardButton(cat.getName(), EXPENSE_CATEGORY_CB_PREFIX + cat.getId());
      rows.add(List.of(btn));
    }
    saveUserState(userState, ConversationState.AWAITING_EXPENSE_CATEGORY_SELECTION, userState.getSelectedAccountId());
    telegramBotClient.sendMessageWithInlineKeyboard(chatId, "Select expense category:", rows);
  }

  /**
   * Handle /addIncome command.
   * Requires account to be selected via /selectAccount first.
   * This command leads to AWAITING_INCOME_CATEGORY_SELECTION.
   * Inline keyboard with income categories is sent to the user (callback query starts).
   */
  public void handleAddIncomeCommand(
      TelegramUser telegramUser,
      TelegramWebhookDto.TelegramMessageDto message,
      TelegramUserState userState
  ) {
    var telegramId = telegramUser.getTelegramId();
    var chatId = message.getChat().getId();
    log.info("Processing /addIncome command from user {}", telegramId);
    saveTelegramMessage(message.getMessageId(), telegramId, chatId, message.getText());

    if (userState.getSelectedAccountId() == null) {
      telegramBotClient.sendMessage(chatId, SELECT_ACCOUNT_FIRST);
      return;
    }

    var categories = incomeCategoryRepository.findAll(
        IncomeCategorySpec.accountIdEq(userState.getSelectedAccountId())
    );

    if (categories.isEmpty()) {
      saveUserState(userState, ConversationState.NONE, userState.getSelectedAccountId());
      telegramBotClient.sendMessage(chatId, "You don't have any income categories yet. Please create one in the app first.");
      return;
    }

    var rows = new ArrayList<List<TelegramBotClient.InlineKeyboardButton>>();
    for (var cat : categories) {
      var btn = new TelegramBotClient.InlineKeyboardButton(cat.getName(), INCOME_CATEGORY_CB_PREFIX + cat.getId());
      rows.add(List.of(btn));
    }
    saveUserState(userState, ConversationState.AWAITING_INCOME_CATEGORY_SELECTION, userState.getSelectedAccountId());
    telegramBotClient.sendMessageWithInlineKeyboard(chatId, "Select income category:", rows);
  }

  /**
   * Handle callback query (account selection, category selection).
   * Callback query is a Telegram feature that allows users to interact with the bot by clicking on buttons in inline keyboard that we sent earlier.
   */
  public void handleCallbackQuery(TelegramWebhookDto.TelegramCallbackQueryDto callback) {
    var telegramId = callback.getFrom().getId();
    var message = callback.getMessage();
    if (message == null) {
      return;
    }
    var chatId = message.getChat().getId();
    var messageId = message.getMessageId();

    var telegramUser = telegramUserRepository.findById(telegramId);
    if (telegramUser.isEmpty() || chatId == null) {
      return;
    }

    if (telegramMessageRepository.existsByMessageId(messageId)) {
      log.debug("Message {} already processed, skipping", messageId);
      return;
    }

    saveTelegramMessage(messageId, telegramId, chatId, callback.getData());

    // User state must exist before processing callback (created on root commands)
    TelegramUserState userState = telegramUserStateRepository.findByIdWithLock(telegramId).orElseThrow();

    var data = callback.getData();
    if (data == null) {
      return;
    }

    if (data.startsWith(SELECT_ACCOUNT_CB_PREFIX)) {
      handleSelectAccountCallback(callback, telegramUser.get(), userState, chatId, data);
    } else if (data.startsWith(EXPENSE_CATEGORY_CB_PREFIX)) {
      handleExpenseCategoryCallback(callback, telegramUser.get(), userState, chatId, data);
    } else if (data.startsWith(INCOME_CATEGORY_CB_PREFIX)) {
      handleIncomeCategoryCallback(callback, telegramUser.get(), userState, chatId, data);
    }
  }

  /**
   * Handle report dates input from user after /report command.
   * Requires account to be selected via /selectAccount first.
   * This command leads to creating report task.
   */
  public void handleReportDatesInput(
      Long telegramId,
      TelegramWebhookDto.TelegramMessageDto message,
      TelegramUserState userState
  ) {
    var chatId = message.getChat().getId();
    var messageText = message.getText();
    log.info("Processing date input from user {}: {}", telegramId, messageText);
    saveTelegramMessage(message.getMessageId(), telegramId, chatId, messageText);

    if (userState.getSelectedAccountId() == null) {
      telegramBotClient.sendMessage(chatId, SELECT_ACCOUNT_FIRST);
      return;
    }

    // Validate date format
    var dateInput = validateAndGetDates(telegramId, messageText, chatId);
    if (dateInput == null) {
      return;
    }

    var reportTask = ReportTask.builder()
        .telegramId(telegramId)
        .chatId(chatId)
        .accountId(userState.getSelectedAccountId())
        .startDate(dateInput.startDate())
        .endDate(dateInput.endDate())
        .status(ReportTaskStatus.PENDING)
        .retryCount(0)
        .maxRetries(maxRetries)
        .createdAt(LocalDateTime.now())
        .nextRetryAt(LocalDateTime.now())
        .build();
    reportTaskRepository.save(reportTask);

    saveUserState(userState, ConversationState.NONE, userState.getSelectedAccountId());

    // Send success message
    telegramBotClient.sendMessage(
        chatId,
        """
            ✅ Your report has been queued successfully!
            
            The report will be generated and sent to you within a few minutes.
            Please be patient."""
    );

    log.info("Report task created successfully for user {}: {} to {}", telegramId, dateInput.startDate(), dateInput.endDate());
  }


  /**
   * Handle expense input from user after /addExpense command.
   * Requires category to be selected via /selectCategory first.
   * This command leads to creating expense.
   */
  public void handleExpenseInput(
      TelegramUser telegramUser,
      TelegramWebhookDto.TelegramMessageDto message,
      TelegramUserState userState
  ) {
    var telegramId = telegramUser.getTelegramId();
    var chatId = message.getChat().getId();
    var messageText = message.getText();
    log.info("Processing expense input from user {}: {}", telegramId, messageText);
    saveTelegramMessage(message.getMessageId(), telegramId, chatId, messageText);

    if (userState.getSelectedCategoryId() == null || userState.getSelectedAccountId() == null) {
      telegramBotClient.sendMessage(chatId, "Please start over with /addExpense command.");
      saveUserState(userState, ConversationState.NONE, userState.getSelectedAccountId());
      return;
    }

    var operationInput = parseOperationInput(messageText, chatId);
    if (operationInput == null) {
      return;
    }

    saveOperation(expenseService, telegramUser, userState, operationInput, chatId);
  }

  /**
   * Handle income input from user.
   * Requires category to be selected via /selectCategory first.
   * This command leads to creating income.
   */
  public void handleIncomeInput(
      TelegramUser telegramUser,
      TelegramWebhookDto.TelegramMessageDto message,
      TelegramUserState userState
  ) {
    var telegramId = telegramUser.getTelegramId();
    var chatId = message.getChat().getId();
    var messageText = message.getText();
    log.info("Processing income input from user {}: {}", telegramId, messageText);
    saveTelegramMessage(message.getMessageId(), telegramId, chatId, messageText);

    if (userState.getSelectedCategoryId() == null || userState.getSelectedAccountId() == null) {
      telegramBotClient.sendMessage(chatId, "Please start over with /addIncome command.");
      saveUserState(userState, ConversationState.NONE, userState.getSelectedAccountId());
      return;
    }

    var operationInput = parseOperationInput(messageText, chatId);
    if (operationInput == null) {
      return;
    }

    saveOperation(incomeService, telegramUser, userState, operationInput, chatId);
  }

  /**
   * Handle select account callback.
   * This callback leads to NONE.
   * Selected account is saved to user state.
   */
  private void handleSelectAccountCallback(
      TelegramWebhookDto.TelegramCallbackQueryDto callback,
      TelegramUser telegramUser,
      TelegramUserState userState,
      Long chatId,
      String data
  ) {
    var accountId = validateAndGetAccountId(data, telegramUser, chatId);
    if (accountId.isEmpty()) {
      return;
    }

    var accountOpt = accountRepository.findByIdAndUserId(accountId.get(), telegramUser.getUser().getId());
    if (accountOpt.isEmpty()) {
      telegramBotClient.sendMessage(chatId, "Selected account is not available. Please try /selectAccount again.");
      return;
    }

    saveUserState(userState, ConversationState.NONE, accountId.get(), null);
    telegramBotClient.answerCallbackQuery(callback.getId());
    telegramBotClient.sendMessage(chatId, "Account '" + accountOpt.get().getName() + "' has been selected.");
  }

  /**
   * Handle expense category selection callback.
   * This callback leads to AWAITING_EXPENSE_INPUT.
   * Instructions to enter expense details are sent to the user.
   */
  private void handleExpenseCategoryCallback(
      TelegramWebhookDto.TelegramCallbackQueryDto callback,
      TelegramUser telegramUser,
      TelegramUserState userState,
      Long chatId,
      String data
  ) {
    var categoryId = validateAndGetCategoryId(data, EXPENSE_CATEGORY_CB_PREFIX, telegramUser, chatId, userState.getSelectedAccountId());
    if (categoryId.isEmpty()) {
      return;
    }

    saveUserState(userState, ConversationState.AWAITING_EXPENSE_INPUT, userState.getSelectedAccountId(), categoryId.get());
    telegramBotClient.answerCallbackQuery(callback.getId());
    telegramBotClient.sendMessage(chatId,
        "Please enter expense details in format:\nvalue;date;description\n\n"
            + "Examples:\n"
            + "• 100.50\n"
            + "• 100.50;16.11.2025\n"
            + "• 100.50;16.11.2025;Coffee\n"
            + "• 100.50;;Coffee (date will be today)\n\n"
            + "Date format: DD.MM.YYYY\n"
            + "Second and third fields are optional.");
  }

  /**
   * Handle income category selection callback.
   * This callback leads to AWAITING_INCOME_INPUT.
   * Instructions to enter income details are sent to the user.
   */
  private void handleIncomeCategoryCallback(
      TelegramWebhookDto.TelegramCallbackQueryDto callback,
      TelegramUser telegramUser,
      TelegramUserState userState,
      Long chatId,
      String data
  ) {
    var categoryId = validateAndGetCategoryId(data, INCOME_CATEGORY_CB_PREFIX, telegramUser, chatId, userState.getSelectedAccountId());
    if (categoryId.isEmpty()) {
      return;
    }

    saveUserState(userState, ConversationState.AWAITING_INCOME_INPUT, userState.getSelectedAccountId(), categoryId.get());
    telegramBotClient.answerCallbackQuery(callback.getId());
    telegramBotClient.sendMessage(chatId,
        "Please enter income details in format:\nvalue;date;description\n\n"
            + "Examples:\n"
            + "• 1000\n"
            + "• 1000;16.11.2025\n"
            + "• 1000;16.11.2025;Salary\n"
            + "• 1000;;Salary (date will be today)\n\n"
            + "Date format: DD.MM.YYYY\n"
            + "Second and third fields are optional.");
  }

  /**
   * Parse operation input in format: value;date;description
   */
  private OperationInput parseOperationInput(String input, Long chatId) {
    var parts = input.split(";", -1);

    if (parts.length == 0 || parts.length > 3) {
      telegramBotClient.sendMessage(chatId,
          "Invalid format. Please use: value;date;description\n"
              + "Date and description are optional.\n"
              + "Example: 100.50;16.11.2025;Coffee");
      return null;
    }

    // Parse value
    BigDecimal value;
    try {
      value = new BigDecimal(parts[0].trim());
      if (value.compareTo(BigDecimal.ZERO) <= 0) {
        telegramBotClient.sendMessage(chatId, "Error: Value must be positive.");
        return null;
      }
    } catch (NumberFormatException e) {
      telegramBotClient.sendMessage(chatId, "Error: Invalid value format. Please enter a number.");
      return null;
    }

    // Parse date (optional)
    LocalDate date = LocalDate.now();
    if (parts.length >= 2 && !parts[1].trim().isEmpty()) {
      try {
        date = LocalDate.parse(parts[1].trim(), DATE_FORMATTER);
      } catch (DateTimeParseException e) {
        telegramBotClient.sendMessage(chatId, "Error: Invalid date format. Please use DD.MM.YYYY format.");
        return null;
      }
    }

    // Parse description (optional)
    String description = null;
    if (parts.length == 3 && !parts[2].trim().isEmpty()) {
      description = parts[2].trim();
    }

    return new OperationInput(value, date, description);
  }

  private DateInput validateAndGetDates(Long telegramId, String messageText, Long chatId) {
    var matcher = DATE_RANGE_PATTERN.matcher(messageText.trim());
    if (!matcher.matches()) {
      telegramBotClient.sendMessage(chatId, "Invalid format. Please use DD.MM.YYYY-DD.MM.YYYY format.\nExample: 01.01.2024-31.12.2024");
      return null;
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
      return null;
    }

    // Validate date range
    if (startDate.isAfter(endDate)) {
      telegramBotClient.sendMessage(chatId, "Error: Start date must be before end date.");
      return null;
    }
    if (ChronoUnit.DAYS.between(startDate, endDate) > 365) {
      telegramBotClient.sendMessage(chatId, "Error: Date range cannot exceed 1 year (365 days).");
      return null;
    }
    return new DateInput(startDate, endDate);
  }

  private Optional<Long> validateAndGetAccountId(String data, TelegramUser telegramUser, Long chatId) {
    Long accountId;
    try {
      accountId = Long.parseLong(data.substring(SELECT_ACCOUNT_CB_PREFIX.length()));
    } catch (NumberFormatException ex) {
      log.warn("Invalid callback data from user {}: {}", telegramUser.getTelegramId(), data);
      return Optional.empty();
    }

    // Validate that account belongs to the user
    var accountOpt = accountRepository.findByIdAndUserId(accountId, telegramUser.getUser().getId());
    if (accountOpt.isEmpty()) {
      telegramBotClient.sendMessage(chatId, "Selected account is not available.");
      return Optional.empty();
    }
    return Optional.of(accountId);
  }

  private Optional<Long> validateAndGetCategoryId(String data, String prefix, TelegramUser telegramUser, Long chatId, Long accountId) {
    Long categoryId;
    try {
      categoryId = Long.parseLong(data.substring(prefix.length()));
    } catch (NumberFormatException ex) {
      log.warn("Invalid callback data from user {}: {}", telegramUser.getTelegramId(), data);
      return Optional.empty();
    }

    // Validate that category belongs to the account
    if (prefix.equals(EXPENSE_CATEGORY_CB_PREFIX)) {
      var categoryOpt = expenseCategoryRepository.findByIdAndAccountId(categoryId, accountId);
      if (categoryOpt.isEmpty()) {
        telegramBotClient.sendMessage(chatId, "Selected category is not available. Please try again.");
        return Optional.empty();
      }
    } else if (prefix.equals(INCOME_CATEGORY_CB_PREFIX)) {
      var categoryOpt = incomeCategoryRepository.findByIdAndAccountId(categoryId, accountId);
      if (categoryOpt.isEmpty()) {
        telegramBotClient.sendMessage(chatId, "Selected category is not available. Please try again.");
        return Optional.empty();
      }
    }

    return Optional.of(categoryId);
  }

  private void saveOperation(
      BaseOperationService<? extends BaseOperation, ? extends BaseOperationCategory> operationService,
      TelegramUser telegramUser,
      TelegramUserState userState,
      OperationInput operationInput,
      Long chatId
  ) {
    try {
      // Set security context for the service call
      setSecurityContext(telegramUser.getUser());

      var dto = new OperationRequestDto();
      dto.setCategoryId(userState.getSelectedCategoryId());
      dto.setValue(operationInput.value());
      dto.setDate(operationInput.date());
      dto.setDescription(operationInput.description());
      dto.setIsPlanned(false);

      operationService.create(dto);

      saveUserState(userState, ConversationState.NONE, userState.getSelectedAccountId(), null);
      telegramBotClient.sendMessage(chatId, "✅ Operation added successfully!");
      log.info("Operation created successfully for user {}", telegramUser.getTelegramId());
    } catch (Exception e) {
      log.error("Failed to create operation for user {}", telegramUser.getTelegramId(), e);
      telegramBotClient.sendMessage(chatId, "Error: Failed to add operation. " + e.getMessage());
      saveUserState(userState, ConversationState.NONE, userState.getSelectedAccountId(), null);
    } finally {
      // Clear security context
      SecurityContextHolder.clearContext();
    }
  }

  private void saveUserState(TelegramUserState userState, ConversationState state, Long accountId) {
    saveUserState(userState, state, accountId, null);
  }

  private void saveUserState(TelegramUserState userState, ConversationState state, Long accountId, Long categoryId) {
    userState.setSelectedAccountId(accountId);
    userState.setSelectedCategoryId(categoryId);
    userState.setState(state);
    userState.setUpdatedAt(LocalDateTime.now());
    telegramUserStateRepository.save(userState);
  }

  private void saveTelegramMessage(Long messageId, Long telegramId, Long chatId, String data) {
    telegramMessageRepository.save(TelegramMessage.builder()
        .messageId(messageId)
        .telegramId(telegramId)
        .chatId(chatId)
        .messageText(data)
        .processedAt(LocalDateTime.now())
        .build());
  }

  /**
   * Set security context for the given user to enable service calls that require authenticated user.
   */
  private void setSecurityContext(User user) {
    var principal = UserPrincipal.create(user);
    var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private record DateInput(LocalDate startDate, LocalDate endDate) {
  }

  private record OperationInput(BigDecimal value, LocalDate date, String description) {
  }
}
