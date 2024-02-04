package ru.rgasymov.moneymanager.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.rgasymov.moneymanager.domain.entity.HistoryAction;

public interface HistoryRepository extends JpaRepository<HistoryAction, Long> {

  List<HistoryAction> findAllByAccountId(Long accountId);

  void deleteAllByAccountId(Long accountId);
}
