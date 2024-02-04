package ru.rgasymov.moneymanager.domain.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import ru.rgasymov.moneymanager.constant.DateTimeFormats;
import ru.rgasymov.moneymanager.domain.enums.Period;
import ru.rgasymov.moneymanager.domain.enums.SavingFieldToSort;

@Data
@NoArgsConstructor
public class SavingCriteriaDto {

  @DateTimeFormat(pattern = DateTimeFormats.COMMON_DATE_FORMAT)
  private LocalDate from;

  @DateTimeFormat(pattern = DateTimeFormats.COMMON_DATE_FORMAT)
  private LocalDate to;

  private SavingFieldToSort sortBy = SavingFieldToSort.DATE;

  private Sort.Direction sortDirection = Sort.Direction.ASC;

  @Parameter(description = "Page number starting at 0", example = "0")
  @PositiveOrZero
  private Integer pageNum = 0;

  @Parameter(example = "1000")
  @Positive
  private Integer pageSize = 1000;

  private Period groupBy = Period.DAY;

  private List<Long> incomeCategoryIds = List.of();

  private List<Long> expenseCategoryIds = List.of();

  private String searchText;

  public void setFrom(LocalDate from) {
    if (to != null && to.isBefore(from)) {
      throw new ValidationException("Date 'from' cannot be after 'to'");
    }
    this.from = from;
  }

  public void setTo(LocalDate to) {
    if (from != null && from.isAfter(to)) {
      throw new ValidationException("Date 'to' cannot be before 'from'");
    }
    this.to = to;
  }
}
