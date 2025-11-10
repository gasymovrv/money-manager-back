package ru.rgasymov.moneymanager.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Telegram Login Widget authentication data.
 */
@Data
@NoArgsConstructor
public class TelegramAuthDto {

  @NotNull
  private Long id;

  private String firstName;

  private String lastName;

  private String username;

  private String photoUrl;

  @NotNull
  private Long authDate;

  @NotNull
  private String hash;
}
