package ru.rgasymov.moneymanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Entity for storing processed Telegram messages to prevent duplicate processing.
 */
@Entity
@Table(name = "telegram_messages")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class TelegramMessage implements Serializable {
  @Serial
  private static final long serialVersionUID = 1234567L;

  @Id
  @Column(name = "message_id")
  @ToString.Include
  private Long messageId;

  @Column(name = "telegram_id", nullable = false)
  private Long telegramId;

  @Column(name = "chat_id", nullable = false)
  private Long chatId;

  @Column(name = "message_text", columnDefinition = "TEXT")
  private String messageText;

  @Column(name = "processed_at", nullable = false)
  private LocalDateTime processedAt;
}
