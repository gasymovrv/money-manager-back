package ru.rgasymov.moneymanager.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.rgasymov.moneymanager.domain.entity.HistoryAction;

public interface HistoryRepository extends JpaRepository<HistoryAction, Long> {

  Page<HistoryAction> findAllByAccountIdOrderByModifiedAtDesc(Long accountId, Pageable pageable);

  void deleteAllByAccountId(Long accountId);
}
