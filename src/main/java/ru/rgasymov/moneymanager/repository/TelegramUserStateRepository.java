package ru.rgasymov.moneymanager.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rgasymov.moneymanager.domain.entity.TelegramUserState;

/**
 * Repository for TelegramUserState entity.
 */
@Repository
public interface TelegramUserStateRepository extends JpaRepository<TelegramUserState, Long> {

  /**
   * Find user state with pessimistic lock to prevent race conditions.
   *
   * @param telegramId the telegram user ID
   * @return optional of TelegramUserState
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM TelegramUserState s WHERE s.telegramId = :telegramId")
  Optional<TelegramUserState> findByIdWithLock(@Param("telegramId") Long telegramId);
}
