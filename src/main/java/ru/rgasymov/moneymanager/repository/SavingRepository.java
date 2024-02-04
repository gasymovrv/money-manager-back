package ru.rgasymov.moneymanager.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rgasymov.moneymanager.domain.entity.Saving;

public interface SavingRepository
    extends JpaRepository<Saving, Long>, JpaSpecificationExecutor<Saving> {

  Optional<Saving> findByDateAndAccountId(LocalDate date, Long accountId);

  Optional<Saving> findFirstByDateLessThanAndAccountIdOrderByDateDesc(LocalDate date,
                                                                      Long accountId);

  @Modifying
  @Query("""
      update Saving a
      set a.value = a.value + :increment
      where a.date > :date and a.accountId = :accountId
      """)
  void increaseValueByDateGreaterThan(@Param("increment") BigDecimal increment,
                                      @Param("date") LocalDate date,
                                      @Param("accountId") Long accountId);

  @Modifying
  @Query("""
      update Saving a
      set a.value = a.value - :decrement
      where a.date > :date and a.accountId = :accountId
      """)
  void decreaseValueByDateGreaterThan(@Param("decrement") BigDecimal decrement,
                                      @Param("date") LocalDate date,
                                      @Param("accountId") Long accountId);

  void deleteAllByAccountId(Long accountId);

  boolean existsByAccountId(Long accountId);
}
