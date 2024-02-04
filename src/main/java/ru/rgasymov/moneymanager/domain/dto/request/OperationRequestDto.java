package ru.rgasymov.moneymanager.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationRequestDto {

  @NotNull
  private Long categoryId;

  @NotNull
  private LocalDate date;

  private String description;

  @Positive
  @NotNull
  private BigDecimal value;

  @NotNull
  private Boolean isPlanned;

  public void setIsPlanned(Boolean isPlanned) {
    LocalDate now = LocalDate.now();
    if (date != null && Boolean.TRUE.equals(isPlanned)
        && (date.isBefore(now) || date.isEqual(now))) {
      throw new ValidationException("'isPlanned' cannot be true if 'date' in the past or today");
    } else {
      this.isPlanned = isPlanned;
    }
  }

  public void setDate(LocalDate date) {
    LocalDate now = LocalDate.now();
    if (Boolean.TRUE.equals(isPlanned) && (date.isBefore(now) || date.isEqual(now))) {
      throw new ValidationException("'date' cannot be in the past or today if 'isPlanned' = true");
    } else {
      this.date = date;
    }
  }
}
