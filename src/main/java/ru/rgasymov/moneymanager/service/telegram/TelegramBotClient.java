package ru.rgasymov.moneymanager.service.telegram;

import java.io.File;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Client for interacting with Telegram Bot API.
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
      String url = String.format(TELEGRAM_API_URL, botToken, "sendMessage");

      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("chat_id", chatId);
      body.add("text", text);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

      ResponseEntity<String> response = restTemplate.exchange(
          url,
          HttpMethod.POST,
          requestEntity,
          String.class
      );

      log.debug("Sent message to chat {}: {}", chatId, response.getStatusCode());
      if (!response.getStatusCode().is2xxSuccessful()) {
        // TODO Лучше наверно тут кидать exception чтобы транзакция откатывалась в случае ошибок а не повисала в состоянии о котором юзер не узнал?
      }
    } catch (Exception e) {
      log.error("Failed to send message to chat {}", chatId, e);
      throw e;
    }
  }

  /**
   * Send document file to a chat.
   *
   * @param chatId   the chat ID
   * @param file     the file to send
   * @param caption  optional caption
   * @return true if successful
   */
  public boolean sendDocument(Long chatId, File file, String caption) {
    try {
      String url = String.format(TELEGRAM_API_URL, botToken, "sendDocument");

      byte[] fileContent = Files.readAllBytes(file.toPath());

      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("chat_id", chatId);
      body.add("document", new ByteArrayResource(fileContent) {
        @Override
        public String getFilename() {
          return file.getName();
        }
      });

      if (caption != null && !caption.isEmpty()) {
        body.add("caption", caption);
      }

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);

      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

      ResponseEntity<String> response = restTemplate.exchange(
          url,
          HttpMethod.POST,
          requestEntity,
          String.class
      );

      log.debug("Sent document to chat {}: {}", chatId, response.getStatusCode());
      return response.getStatusCode().is2xxSuccessful();
    } catch (Exception e) {
      log.error("Failed to send document to chat {}", chatId, e);
      return false;
    }
  }
}
