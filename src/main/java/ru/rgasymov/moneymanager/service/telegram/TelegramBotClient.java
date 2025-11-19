package ru.rgasymov.moneymanager.service.telegram;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Client for interacting with Telegram Bot API.
 * <br/>
 * To send requests to the Telegram Bot API, you need to use the bot token in API url.
 * <br/>
 * Telegram docs:
 * <br/>
 * <a href="https://core.telegram.org/bots/api#authorizing-your-bot">Authorizing your bot</a>
 * <br/>
 * <a href="https://core.telegram.org/bots/api#available-methods">Available methods</a>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramBotClient {

  private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot%s/%s";

  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${telegram.bot.token}")
  private String botToken;

  /**
   * Send text message to a chat.
   *
   * @param chatId the chat ID
   * @param text   the message text
   */
  public void sendMessage(Long chatId, String text) {
    try {
      var headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      var requestEntity = new HttpEntity<>(new SendMessageRequest(chatId, text), headers);

      var statusCode = send("sendMessage", requestEntity);

      log.debug("Sent message to chat {}: {}", chatId, statusCode);
    } catch (Exception e) {
      log.error("Failed to send message to chat {}", chatId, e);
      throw e;
    }
  }

  /**
   * Send text message with inline keyboard to a chat.
   *
   * @param chatId  the chat ID
   * @param text    the message text
   * @param buttons inline keyboard buttons (rows)
   */
  public void sendMessageWithInlineKeyboard(Long chatId, String text, List<List<InlineKeyboardButton>> buttons) {
    try {
      var headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      var replyMarkup = new InlineKeyboardMarkup(buttons);
      var requestEntity = new HttpEntity<>(new SendMessageWithMarkupRequest(chatId, text, replyMarkup), headers);

      var statusCode = send("sendMessage", requestEntity);

      log.debug("Sent message with inline keyboard to chat {}: {}", chatId, statusCode);
    } catch (Exception e) {
      log.error("Failed to send message with inline keyboard to chat {}", chatId, e);
      throw e;
    }
  }

  /**
   * Answer a callback query to remove 'loading' state in Telegram client.
   *
   * @param callbackQueryId callback query ID from update
   */
  public void answerCallbackQuery(String callbackQueryId) {
    try {
      var headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      var requestEntity = new HttpEntity<>(new AnswerCallbackQueryRequest(callbackQueryId), headers);

      var statusCode = send("answerCallbackQuery", requestEntity);

      log.debug("Answered callback query {}: {}", callbackQueryId, statusCode);
    } catch (Exception e) {
      log.error("Failed to answer callback query {}", callbackQueryId, e);
      throw e;
    }
  }

  /**
   * Send document file to a chat.
   *
   * @param chatId  the chat ID
   * @param file    the file to send
   * @param caption optional caption
   */
  public void sendDocument(Long chatId, File file, String caption) {
    try {
      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("chat_id", chatId.toString());
      body.add("document", new FileSystemResource(file));

      if (caption != null && !caption.isEmpty()) {
        body.add("caption", caption);
      }

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);
      var requestEntity = new HttpEntity<>(body, headers);

      var statusCode = send("sendDocument", requestEntity);

      log.debug("Sent document to chat {}: {}", chatId, statusCode);
    } catch (Exception e) {
      log.error("Failed to send document to chat {}", chatId, e);
      throw e;
    }
  }

  /**
   * Send text message to a chat with retry logic.
   * Retries on 5xx errors or timeouts.
   *
   * @param chatId the chat ID
   * @param text   the message text
   */
  @Retryable(
      retryFor = {HttpServerErrorException.class, ResourceAccessException.class},
      maxAttemptsExpression = "${telegram.bot.retry.max-attempts:3}",
      backoff = @Backoff(delayExpression = "#{${telegram.bot.retry.delay-seconds:5} * 1000}")
  )
  public void sendMessageWithRetry(Long chatId, String text) {
    sendMessage(chatId, text);
  }

  /**
   * Send document file to a chat with retry logic.
   * Retries on 5xx errors or timeouts.
   *
   * @param chatId  the chat ID
   * @param file    the file to send
   * @param caption optional caption
   */
  @Retryable(
      retryFor = {HttpServerErrorException.class, ResourceAccessException.class},
      maxAttemptsExpression = "${telegram.bot.retry.max-attempts:3}",
      backoff = @Backoff(delayExpression = "#{${telegram.bot.retry.delay-seconds:5} * 1000}")
  )
  public void sendDocumentWithRetry(Long chatId, File file, String caption) {
    sendDocument(chatId, file, caption);
  }

  private <T> HttpStatusCode send(String endpointName, HttpEntity<T> requestEntity) {
    var statusCode = restTemplate.exchange(
        String.format(TELEGRAM_API_URL, botToken, endpointName),
        HttpMethod.POST,
        requestEntity,
        String.class
    ).getStatusCode();

    if (statusCode.is5xxServerError()) {
      throw new HttpServerErrorException(statusCode);
    } else if (statusCode.is4xxClientError()) {
      throw new HttpClientErrorException(statusCode);
    }
    return statusCode;
  }

  private record AnswerCallbackQueryRequest(
      @JsonProperty("callback_query_id") String callbackQueryId
  ) {
  }

  private record SendMessageRequest(
      @JsonProperty("chat_id") Long chatId,
      String text
  ) {
  }

  private record SendMessageWithMarkupRequest(
      @JsonProperty("chat_id") Long chatId,
      String text,
      @JsonProperty("reply_markup") InlineKeyboardMarkup replyMarkup
  ) {
  }

  private record InlineKeyboardMarkup(
      @JsonProperty("inline_keyboard") List<List<InlineKeyboardButton>> inlineKeyboard
  ) {
  }

  public record InlineKeyboardButton(
      String text,
      @JsonProperty("callback_data") String callbackData
  ) {
  }
}
