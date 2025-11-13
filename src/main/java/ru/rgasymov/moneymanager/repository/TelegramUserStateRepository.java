package ru.rgasymov.moneymanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rgasymov.moneymanager.domain.entity.TelegramUserState;

/**
 * Repository for TelegramUserState entity.
 */
@Repository
public interface TelegramUserStateRepository extends JpaRepository<TelegramUserState, Long> {
}
