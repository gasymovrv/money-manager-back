package ru.rgasymov.moneymanager.mapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rgasymov.moneymanager.domain.dto.response.OperationResponseDto;
import ru.rgasymov.moneymanager.domain.dto.response.SavingResponseDto;
import ru.rgasymov.moneymanager.domain.entity.Saving;
import ru.rgasymov.moneymanager.domain.enums.Period;

@Component
@RequiredArgsConstructor
public class SavingGroupMapper {
  private final SavingMapper delegate;

  public List<SavingResponseDto> toGroupDtos(List<Saving> entities, Period period) {
    List<SavingResponseDto> dtoList = delegate.toDtos(entities);
    if (period == Period.DAY) {
      return dtoList;
    }

    Map<String, SavingResponseDto> temp = new LinkedHashMap<>();
    for (SavingResponseDto dto : dtoList) {
      LocalDate date = dto.getDate();
      String partOfDate = date.format(DateTimeFormatter.ofPattern(period.getPattern()));

      SavingResponseDto savingGroup = SavingResponseDto
          .builder()
          .id(dto.getId())
          .date(date)
          .value(dto.getValue())
          .period(period)
          .expensesSum(dto.getExpensesSum())
          .incomesSum(dto.getIncomesSum())
          .expensesByCategory(dto.getExpensesByCategory())
          .incomesByCategory(dto.getIncomesByCategory())
          .build();

      temp.merge(partOfDate, savingGroup,
          (oldValue, newValue) -> {
            if (newValue.getDate().isAfter(oldValue.getDate())) {
              oldValue.setId(newValue.getId());
              oldValue.setDate(newValue.getDate());
              oldValue.setValue(newValue.getValue());
            }
            oldValue.setIncomesSum(oldValue.getIncomesSum().add(newValue.getIncomesSum()));
            oldValue.setExpensesSum(oldValue.getExpensesSum().add(newValue.getExpensesSum()));

            for (Map.Entry<String, List<OperationResponseDto>> pair :
                newValue.getIncomesByCategory().entrySet()) {
              oldValue.getIncomesByCategory()
                  .merge(pair.getKey(), pair.getValue(),
                      (o, n) -> {
                        o.addAll(n);
                        return o;
                      });
            }

            for (Map.Entry<String, List<OperationResponseDto>> pair :
                newValue.getExpensesByCategory().entrySet()) {
              oldValue.getExpensesByCategory()
                  .merge(pair.getKey(), pair.getValue(),
                      (o, n) -> {
                        o.addAll(n);
                        return o;
                      });
            }
            return oldValue;
          });
    }

    return new ArrayList<>(temp.values());
  }
}
