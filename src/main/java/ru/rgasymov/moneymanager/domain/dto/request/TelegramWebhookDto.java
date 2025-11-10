package ru.rgasymov.moneymanager.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Telegram webhook updates.
 */
@Data
@NoArgsConstructor
public class TelegramWebhookDto {

  @JsonProperty("update_id")
  private Long updateId;

  private TelegramMessageDto message;

  @Data
  @NoArgsConstructor
  public static class TelegramMessageDto {
    @JsonProperty("message_id")
    private Long messageId;

    private TelegramUserDto from;

    private TelegramChatDto chat;

    private Long date;

    private String text;
  }

  @Data
  @NoArgsConstructor
  public static class TelegramUserDto {
    private Long id;

    @JsonProperty("is_bot")
    private Boolean isBot;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    private String username;

    @JsonProperty("language_code")
    private String languageCode;
  }

  @Data
  @NoArgsConstructor
  public static class TelegramChatDto {
    private Long id;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    private String username;

    private String type;
  }
}
