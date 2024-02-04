package ru.rgasymov.moneymanager.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.rgasymov.moneymanager.constant.DateTimeFormats;

@Schema
@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationResponseDto {

  private Long id;

  private OperationCategoryResponseDto category;

  @JsonFormat(shape = JsonFormat.Shape.STRING,
      pattern = DateTimeFormats.COMMON_DATE_FORMAT)
  private LocalDate date;

  private String description;

  private BigDecimal value;

  @JsonProperty("isPlanned")
  private boolean isPlanned;

  @JsonProperty("isOverdue")
  @Builder.Default
  private boolean isOverdue = false;

  @JsonIgnore
  @Builder.Default
  private boolean isDescriptionCollapsed = false;

  public boolean calculateOverdue(LocalDate referenceDate) {
    if (referenceDate == null || date == null) {
      return isOverdue;
    }
    var isBeforeOrEqualRefDate = (date.isBefore(referenceDate) || date.isEqual(referenceDate));
    if (isBeforeOrEqualRefDate && Boolean.TRUE.equals(isPlanned)) {
      setOverdue(true);
    }
    return isOverdue;
  }
}
