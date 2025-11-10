package ru.rgasymov.moneymanager.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rgasymov.moneymanager.domain.entity.TelegramUser;

/**
 * Repository for TelegramUser entity.
 */
@Repository
public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {

  /**
   * Find Telegram user by user ID.
   *
   * @param userId the user ID
   * @return Optional of TelegramUser
   */
  Optional<TelegramUser> findByUserId(String userId);
}
