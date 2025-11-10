package ru.rgasymov.moneymanager.domain.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Telegram user response.
 */
@Data
@NoArgsConstructor
public class TelegramUserDto {

  private Long telegramId;
  private String firstName;
  private String lastName;
  private String username;
  private String photoUrl;
  private boolean linked;
}
