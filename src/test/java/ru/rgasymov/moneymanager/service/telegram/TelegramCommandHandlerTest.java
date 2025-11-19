package ru.rgasymov.moneymanager.service.telegram;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.rgasymov.moneymanager.domain.dto.request.TelegramWebhookDto;
import ru.rgasymov.moneymanager.domain.dto.response.OperationCategoryResponseDto;
import ru.rgasymov.moneymanager.domain.entity.Account;
import ru.rgasymov.moneymanager.domain.entity.TelegramUser;
import ru.rgasymov.moneymanager.domain.entity.TelegramUserState;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;
import ru.rgasymov.moneymanager.repository.AccountRepository;
import ru.rgasymov.moneymanager.repository.ReportTaskRepository;
import ru.rgasymov.moneymanager.repository.TelegramMessageRepository;
import ru.rgasymov.moneymanager.repository.TelegramUserRepository;
import ru.rgasymov.moneymanager.repository.TelegramUserStateRepository;
import ru.rgasymov.moneymanager.service.expense.ExpenseCategoryService;
import ru.rgasymov.moneymanager.service.expense.ExpenseService;
import ru.rgasymov.moneymanager.service.income.IncomeCategoryService;
import ru.rgasymov.moneymanager.service.income.IncomeService;

@ExtendWith(MockitoExtension.class)
class TelegramCommandHandlerTest {

  @Mock
  private TelegramUserRepository telegramUserRepository;

  @Mock
  private TelegramMessageRepository telegramMessageRepository;

  @Mock
  private TelegramUserStateRepository telegramUserStateRepository;

  @Mock
  private ReportTaskRepository reportTaskRepository;

  @Mock
  private TelegramBotClient telegramBotClient;

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private ExpenseCategoryService expenseCategoryService;

  @Mock
  private IncomeCategoryService incomeCategoryService;

  @Mock
  private ExpenseService expenseService;

  @Mock
  private IncomeService incomeService;

  private TelegramCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new TelegramCommandHandler(
        telegramUserRepository,
        telegramMessageRepository,
        telegramUserStateRepository,
        reportTaskRepository,
        telegramBotClient,
        accountRepository,
        expenseCategoryService,
        incomeCategoryService,
        expenseService,
        incomeService
    );
    ReflectionTestUtils.setField(handler, "maxRetries", 3);
  }

  @Test
  void handleSelectAccountCommand_shouldSendMessage_whenNoAccounts() {
    var telegramUser = createTelegramUser();
    var message = createMessage(123L, "test");
    var userState = new TelegramUserState();

    when(accountRepository.findAllByUserId(anyString())).thenReturn(List.of());

    handler.handleSelectAccountCommand(telegramUser, message, userState);

    verify(telegramBotClient).sendMessage(eq(123L), anyString());
    verify(telegramMessageRepository).save(any());
  }

  @Test
  void handleSelectAccountCommand_shouldSelectAccount_whenOnlyOne() {
    var telegramUser = createTelegramUser();
    var message = createMessage(123L, "/selectAccount");
    var userState = new TelegramUserState();
    var account = Account.builder().id(1L).name("Test Account").build();

    when(accountRepository.findAllByUserId(anyString())).thenReturn(List.of(account));

    handler.handleSelectAccountCommand(telegramUser, message, userState);

    verify(telegramBotClient).sendMessage(eq(123L), anyString());
    verify(telegramUserStateRepository).save(any());
  }

  @Test
  void handleSelectAccountCommand_shouldShowKeyboard_whenMultipleAccounts() {
    var telegramUser = createTelegramUser();
    var message = createMessage(123L, "/selectAccount");
    var userState = new TelegramUserState();
    var account1 = Account.builder().id(1L).name("Account 1").build();
    var account2 = Account.builder().id(2L).name("Account 2").build();

    when(accountRepository.findAllByUserId(anyString())).thenReturn(List.of(account1, account2));

    handler.handleSelectAccountCommand(telegramUser, message, userState);

    verify(telegramBotClient).sendMessageWithInlineKeyboard(eq(123L), anyString(), anyList());
    verify(telegramUserStateRepository).save(any());
  }

  @Test
  void handleReportCommand_shouldRequireAccountSelection() {
    var telegramUser = createTelegramUser();
    var message = createMessage(123L, "/report");
    var userState = new TelegramUserState();
    userState.setSelectedAccountId(null);

    handler.handleReportCommand(telegramUser, message, userState);

    verify(telegramBotClient).sendMessage(eq(123L), anyString());
  }

  @Test
  void handleReportCommand_shouldRequestDates_whenAccountSelected() {
    var telegramUser = createTelegramUser();
    var message = createMessage(123L, "/report");
    var userState = new TelegramUserState();
    userState.setSelectedAccountId(1L);

    handler.handleReportCommand(telegramUser, message, userState);

    verify(telegramBotClient).sendMessage(eq(123L), anyString());
    verify(telegramUserStateRepository).save(any());
  }

  @Test
  void handleAddExpenseCommand_shouldRequireAccountSelection() {
    var telegramUser = createTelegramUser();
    var message = createMessage(123L, "/addExpense");
    var userState = new TelegramUserState();
    userState.setSelectedAccountId(null);

    handler.handleAddExpenseCommand(telegramUser, message, userState);

    verify(telegramBotClient).sendMessage(eq(123L), anyString());
  }

  @Test
  void handleAddExpenseCommand_shouldShowCategories_whenAccountSelected() {
    var telegramUser = createTelegramUser();
    var message = createMessage(123L, "/addExpense");
    var userState = new TelegramUserState();
    userState.setSelectedAccountId(1L);

    var category = new OperationCategoryResponseDto();
    category.setId(1L);
    category.setName("Food");

    when(expenseCategoryService.findAll(1L)).thenReturn(List.of(category));

    handler.handleAddExpenseCommand(telegramUser, message, userState);

    verify(telegramBotClient).sendMessageWithInlineKeyboard(eq(123L), anyString(), anyList());
    verify(telegramUserStateRepository).save(any());
  }

  @Test
  void handleAddExpenseCommand_shouldSendMessage_whenNoCategories() {
    var telegramUser = createTelegramUser();
    var message = createMessage(123L, "/addExpense");
    var userState = new TelegramUserState();
    userState.setSelectedAccountId(1L);

    when(expenseCategoryService.findAll(1L)).thenReturn(List.of());

    handler.handleAddExpenseCommand(telegramUser, message, userState);

    verify(telegramBotClient).sendMessage(eq(123L), anyString());
    verify(telegramUserStateRepository).save(any());
  }

  @Test
  void handleAddIncomeCommand_shouldShowCategories_whenAccountSelected() {
    var telegramUser = createTelegramUser();
    var message = createMessage(123L, "/addIncome");
    var userState = new TelegramUserState();
    userState.setSelectedAccountId(1L);

    var category = new OperationCategoryResponseDto();
    category.setId(1L);
    category.setName("Salary");

    when(incomeCategoryService.findAll(1L)).thenReturn(List.of(category));

    handler.handleAddIncomeCommand(telegramUser, message, userState);

    verify(telegramBotClient).sendMessageWithInlineKeyboard(eq(123L), anyString(), anyList());
    verify(telegramUserStateRepository).save(any());
  }

  @Test
  void handleCallbackQuery_shouldSkip_whenMessageAlreadyProcessed() {
    var callback = createCallback("SELECT_ACC:1", 999L);

    when(telegramUserRepository.findById(anyLong())).thenReturn(Optional.of(createTelegramUser()));
    when(telegramMessageRepository.existsByMessageId(999L)).thenReturn(true);

    handler.handleCallbackQuery(callback);

    verify(telegramBotClient, never()).sendMessage(anyLong(), anyString());
  }

  @Test
  void handleReportDatesInput_shouldValidateDateFormat() {
    var message = createMessage(123L, "invalid-format");
    var userState = new TelegramUserState();
    userState.setSelectedAccountId(1L);

    handler.handleReportDatesInput(123456L, message, userState);

    verify(telegramBotClient).sendMessage(eq(123L), anyString());
  }

  private TelegramUser createTelegramUser() {
    var account = Account.builder()
        .id(1L)
        .name("Test Account")
        .theme(AccountTheme.LIGHT)
        .currency("USD")
        .build();

    var user = User.builder()
        .id("user123")
        .email("test@example.com")
        .name("Test User")
        .provider(AuthProviders.GOOGLE)
        .currentAccount(account)
        .build();

    var telegramUser = new TelegramUser();
    telegramUser.setTelegramId(123456L);
    telegramUser.setUser(user);
    telegramUser.setFirstName("Test");
    return telegramUser;
  }

  private TelegramWebhookDto.TelegramMessageDto createMessage(Long chatId, String text) {
    var message = new TelegramWebhookDto.TelegramMessageDto();
    message.setMessageId(1L);
    message.setText(text);
    var chat = new TelegramWebhookDto.TelegramChatDto();
    chat.setId(chatId);
    message.setChat(chat);
    return message;
  }

  private TelegramWebhookDto.TelegramCallbackQueryDto createCallback(String data, Long messageId) {
    var callback = new TelegramWebhookDto.TelegramCallbackQueryDto();
    callback.setId("callback123");
    callback.setData(data);

    var from = new TelegramWebhookDto.TelegramUserDto();
    from.setId(123456L);
    callback.setFrom(from);

    var message = new TelegramWebhookDto.TelegramMessageDto();
    message.setMessageId(messageId);
    var chat = new TelegramWebhookDto.TelegramChatDto();
    chat.setId(123L);
    message.setChat(chat);
    callback.setMessage(message);

    return callback;
  }
}
