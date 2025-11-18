package ru.rgasymov.moneymanager.service.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.rgasymov.moneymanager.domain.dto.request.TelegramAuthDto;
import ru.rgasymov.moneymanager.domain.dto.request.TelegramWebhookDto;
import ru.rgasymov.moneymanager.domain.entity.Account;
import ru.rgasymov.moneymanager.domain.entity.TelegramUser;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;
import ru.rgasymov.moneymanager.repository.TelegramMessageRepository;
import ru.rgasymov.moneymanager.repository.TelegramUserRepository;
import ru.rgasymov.moneymanager.repository.TelegramUserStateRepository;

@ExtendWith(MockitoExtension.class)
class TelegramServiceTest {

  @Mock
  private TelegramUserRepository telegramUserRepository;

  @Mock
  private TelegramUserStateRepository telegramUserStateRepository;

  @Mock
  private TelegramMessageRepository telegramMessageRepository;

  @Mock
  private TelegramBotClient telegramBotClient;

  @Mock
  private TelegramCommandHandler telegramCommandHandler;

  private TelegramService telegramService;

  @BeforeEach
  void setUp() {
    telegramService = new TelegramService(
        telegramUserRepository,
        telegramUserStateRepository,
        telegramMessageRepository,
        telegramBotClient,
        telegramCommandHandler
    );
    // Set bot token for testing
    ReflectionTestUtils.setField(telegramService, "botToken", "test-bot-token");
  }

  @Test
  void verifyTelegramAuth_shouldReturnFalse_whenAuthDateTooOld() {
    var authDto = new TelegramAuthDto();
    authDto.setId(123456L);
    authDto.setFirstName("Test");
    authDto.setAuthDate(System.currentTimeMillis() / 1000 - 100000); // Too old
    authDto.setHash("somehash");

    var result = telegramService.verifyTelegramAuth(authDto);

    assertThat(result).isFalse();
  }

  @Test
  void verifyTelegramAuth_shouldReturnFalse_whenHashInvalid() {
    var authDto = new TelegramAuthDto();
    authDto.setId(123456L);
    authDto.setFirstName("Test");
    authDto.setAuthDate(System.currentTimeMillis() / 1000); // Recent
    authDto.setHash("invalid-hash");

    var result = telegramService.verifyTelegramAuth(authDto);

    assertThat(result).isFalse();
  }

  @Test
  void linkTelegramUser_shouldCreateNewTelegramUser() {
    var user = createTestUser();
    var authDto = new TelegramAuthDto();
    authDto.setId(123456L);
    authDto.setFirstName("John");
    authDto.setLastName("Doe");
    authDto.setUsername("johndoe");
    authDto.setPhotoUrl("http://example.com/photo.jpg");
    authDto.setAuthDate(System.currentTimeMillis() / 1000);

    when(telegramUserRepository.findById(123456L)).thenReturn(Optional.empty());

    telegramService.linkTelegramUser(authDto, user);

    var captor = ArgumentCaptor.forClass(TelegramUser.class);
    verify(telegramUserRepository).save(captor.capture());

    var savedUser = captor.getValue();
    assertThat(savedUser.getTelegramId()).isEqualTo(123456L);
    assertThat(savedUser.getFirstName()).isEqualTo("John");
    assertThat(savedUser.getLastName()).isEqualTo("Doe");
    assertThat(savedUser.getUsername()).isEqualTo("johndoe");
    assertThat(savedUser.getUser()).isEqualTo(user);
  }

  @Test
  void linkTelegramUser_shouldUpdateExistingTelegramUser() {
    var user = createTestUser();
    var existingTelegramUser = new TelegramUser();
    existingTelegramUser.setTelegramId(123456L);

    var authDto = new TelegramAuthDto();
    authDto.setId(123456L);
    authDto.setFirstName("John");
    authDto.setLastName("Doe");
    authDto.setUsername("johndoe");
    authDto.setPhotoUrl("http://example.com/photo.jpg");
    authDto.setAuthDate(System.currentTimeMillis() / 1000);

    when(telegramUserRepository.findById(123456L)).thenReturn(Optional.of(existingTelegramUser));

    telegramService.linkTelegramUser(authDto, user);

    verify(telegramUserRepository).save(existingTelegramUser);
    assertThat(existingTelegramUser.getFirstName()).isEqualTo("John");
    assertThat(existingTelegramUser.getUser()).isEqualTo(user);
  }

  @Test
  void processWebhook_shouldHandleCallbackQuery() {
    var webhookDto = new TelegramWebhookDto();
    webhookDto.setUpdateId(12345L);
    var callbackQuery = new TelegramWebhookDto.TelegramCallbackQueryDto();
    webhookDto.setCallbackQuery(callbackQuery);

    telegramService.processWebhook(webhookDto);

    verify(telegramCommandHandler).handleCallbackQuery(callbackQuery);
  }

  @Test
  void processWebhook_shouldSkip_whenNoMessage() {
    var webhookDto = new TelegramWebhookDto();
    webhookDto.setUpdateId(12345L);

    telegramService.processWebhook(webhookDto);

    verify(telegramCommandHandler, never()).handleCallbackQuery(any());
  }

  @Test
  void processWebhook_shouldSkip_whenMessageAlreadyProcessed() {
    var webhookDto = new TelegramWebhookDto();
    var message = new TelegramWebhookDto.TelegramMessageDto();
    message.setMessageId(999L);
    webhookDto.setMessage(message);

    when(telegramMessageRepository.existsByMessageId(999L)).thenReturn(true);

    telegramService.processWebhook(webhookDto);

    verify(telegramBotClient, never()).sendMessage(anyLong(), anyString());
  }

  @Test
  void processWebhook_shouldSendLinkMessage_whenUserNotLinked() {
    var webhookDto = new TelegramWebhookDto();
    var message = new TelegramWebhookDto.TelegramMessageDto();
    message.setMessageId(999L);
    var from = new TelegramWebhookDto.TelegramUserDto();
    from.setId(123456L);
    message.setFrom(from);
    var chat = new TelegramWebhookDto.TelegramChatDto();
    chat.setId(789L);
    message.setChat(chat);
    webhookDto.setMessage(message);

    when(telegramMessageRepository.existsByMessageId(999L)).thenReturn(false);
    when(telegramUserRepository.findById(123456L)).thenReturn(Optional.empty());

    telegramService.processWebhook(webhookDto);

    verify(telegramBotClient).sendMessage(eq(789L), anyString());
  }

  private User createTestUser() {
    var account = Account.builder()
        .id(1L)
        .name("Test Account")
        .theme(AccountTheme.LIGHT)
        .currency("USD")
        .build();

    return User.builder()
        .id("user123")
        .email("test@example.com")
        .name("Test User")
        .provider(AuthProviders.GOOGLE)
        .currentAccount(account)
        .build();
  }
}
