package ru.rgasymov.moneymanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rgasymov.moneymanager.domain.entity.TelegramUser;

/**
 * Repository for TelegramUser entity.
 */
@Repository
public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {
}
