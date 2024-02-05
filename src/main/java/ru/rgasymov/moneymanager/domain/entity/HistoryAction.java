package ru.rgasymov.moneymanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import ru.rgasymov.moneymanager.config.JpaConverterJson;
import ru.rgasymov.moneymanager.domain.dto.response.OperationResponseDto;
import ru.rgasymov.moneymanager.domain.enums.HistoryActionType;
import ru.rgasymov.moneymanager.domain.enums.OperationType;

@Entity
@Table(name = "history")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class HistoryAction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @Fetch(FetchMode.JOIN)
  @JoinColumn(name = "account_id")
  private Account account;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_type")
  private HistoryActionType actionType;

  @Enumerated(EnumType.STRING)
  @Column(name = "operation_type")
  private OperationType operationType;

  @Column(name = "modified_at")
  @Builder.Default
  private LocalDateTime modifiedAt = LocalDateTime.now();

  @Convert(converter = JpaConverterJson.class)
  @Column(name = "old_operation")
  private OperationResponseDto oldOperation;

  @Convert(converter = JpaConverterJson.class)
  @Column(name = "new_operation")
  private OperationResponseDto newOperation;
}
