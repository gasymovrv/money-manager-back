package ru.rgasymov.moneymanager.mapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import ru.rgasymov.moneymanager.domain.dto.response.OperationResponseDto;
import ru.rgasymov.moneymanager.domain.dto.response.SavingResponseDto;
import ru.rgasymov.moneymanager.domain.entity.Saving;

@NoArgsConstructor
public class SavingMapperDecorator implements SavingMapper {

  @Autowired
  private IncomeMapper incomeMapper;

  @Autowired
  private ExpenseMapper expenseMapper;

  @Autowired
  private SavingMapper delegate;

  @Override
  public SavingResponseDto toDto(Saving entity) {
    SavingResponseDto dto = delegate.toDto(entity);
    var isOverdue = new AtomicBoolean(false);
    var now = LocalDate.now();

    var expenseMap = new HashMap<String, List<OperationResponseDto>>();

    entity.getExpenses().forEach((exp) -> {
      OperationResponseDto operationDto = expenseMapper.toDto(exp);
      if (operationDto.calculateOverdue(now)) {
        isOverdue.set(true);
      }
      dto.setExpensesSum(dto.getExpensesSum().add(exp.getValue()));

      ArrayList<OperationResponseDto> value = new ArrayList<>();
      value.add(operationDto);
      expenseMap.merge(exp.getCategory().getName(), value,
          (oldValue, newValue) -> {
            oldValue.addAll(newValue);
            return oldValue;
          });
    });

    var incomeMap = new HashMap<String, List<OperationResponseDto>>();

    entity.getIncomes().forEach((inc) -> {
      OperationResponseDto operationDto = incomeMapper.toDto(inc);
      if (operationDto.calculateOverdue(now)) {
        isOverdue.set(true);
      }
      dto.setIncomesSum(dto.getIncomesSum().add(inc.getValue()));

      ArrayList<OperationResponseDto> value = new ArrayList<>();
      value.add(operationDto);
      incomeMap.merge(inc.getCategory().getName(), value,
          (oldValue, newValue) -> {
            oldValue.addAll(newValue);
            return oldValue;
          });
    });

    dto.setOverdue(isOverdue.get());
    dto.setExpensesByCategory(expenseMap);
    dto.setIncomesByCategory(incomeMap);
    return dto;
  }

  @Override
  public List<SavingResponseDto> toDtos(List<Saving> entities) {
    if (entities == null) {
      return null;
    }

    var list = new ArrayList<SavingResponseDto>(entities.size());
    for (Saving saving : entities) {
      list.add(toDto(saving));
    }

    return list;
  }
}
