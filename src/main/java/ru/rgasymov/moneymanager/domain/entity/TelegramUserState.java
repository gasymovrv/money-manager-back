package ru.rgasymov.moneymanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Entity for tracking Telegram user conversation states.
 */
@Entity
@Table(name = "telegram_user_states")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class TelegramUserState implements Serializable {
  @Serial
  private static final long serialVersionUID = 1234569L;

  @Id
  @Column(name = "telegram_id")
  @ToString.Include
  private Long telegramId;

  @Enumerated(EnumType.STRING)
  @Column(name = "state", nullable = false, length = 50)
  private ConversationState state;

  @Column(name = "selected_account_id")
  private Long selectedAccountId;

  @Column(name = "selected_category_id")
  private Long selectedCategoryId;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Conversation state for user interaction flow.
   */
  public enum ConversationState {
    NONE,
    AWAITING_ACCOUNT_SELECTION,
    AWAITING_REPORT_DATES,
    AWAITING_EXPENSE_CATEGORY_SELECTION,
    AWAITING_EXPENSE_INPUT,
    AWAITING_INCOME_CATEGORY_SELECTION,
    AWAITING_INCOME_INPUT
  }
}
