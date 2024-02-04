package ru.rgasymov.moneymanager.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.rgasymov.moneymanager.domain.enums.HistoryActionType;
import ru.rgasymov.moneymanager.domain.enums.OperationType;

@Schema
@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoryActionDto {

  private Long id;

  private AccountResponseDto account;

  private HistoryActionType actionType;

  private OperationType operationType;

  private LocalDateTime modifiedAt;

  private OperationResponseDto oldOperation;

  private OperationResponseDto newOperation;
}
