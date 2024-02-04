package ru.rgasymov.moneymanager.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.rgasymov.moneymanager.domain.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

  Optional<Account> findByIdAndUserId(Long id, String userId);

  List<Account> findAllByUserId(String userId);
}
