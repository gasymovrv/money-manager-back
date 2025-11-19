package ru.rgasymov.moneymanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rgasymov.moneymanager.domain.entity.TelegramMessage;

/**
 * Repository for TelegramMessage entity.
 */
@Repository
public interface TelegramMessageRepository extends JpaRepository<TelegramMessage, Long> {

  /**
   * Check if message exists by message ID.
   *
   * @param messageId the message ID
   * @return true if exists
   */
  boolean existsByMessageId(Long messageId);
}
