package ru.rgasymov.moneymanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
@SuperBuilder
public abstract class BaseOperation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "account_id")
  private Long accountId;

  @Column(name = "saving_id")
  private Long savingId;

  private LocalDate date;

  private String description;

  @Column(name = "value_")
  private BigDecimal value;

  @Column(name = "is_planned")
  private Boolean isPlanned;
}
