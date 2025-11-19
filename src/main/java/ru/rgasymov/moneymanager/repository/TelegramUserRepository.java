package ru.rgasymov.moneymanager.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rgasymov.moneymanager.domain.entity.TelegramUser;

/**
 * Repository for TelegramUser entity.
 */
@Repository
public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {

  /**
   * Find telegram user with pessimistic lock to prevent race conditions.
   *
   * @param telegramId the telegram user ID
   * @return optional of TelegramUser
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT tu FROM TelegramUser tu WHERE tu.telegramId = :telegramId")
  Optional<TelegramUser> findByIdWithLock(@Param("telegramId") Long telegramId);
}
