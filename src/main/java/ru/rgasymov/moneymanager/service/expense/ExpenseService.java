package ru.rgasymov.moneymanager.service.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.rgasymov.moneymanager.domain.dto.request.OperationRequestDto;
import ru.rgasymov.moneymanager.domain.dto.response.OperationResponseDto;
import ru.rgasymov.moneymanager.domain.entity.Expense;
import ru.rgasymov.moneymanager.domain.entity.ExpenseCategory;
import ru.rgasymov.moneymanager.domain.enums.OperationType;
import ru.rgasymov.moneymanager.mapper.ExpenseMapper;
import ru.rgasymov.moneymanager.repository.ExpenseCategoryRepository;
import ru.rgasymov.moneymanager.repository.ExpenseRepository;
import ru.rgasymov.moneymanager.service.BaseOperationService;
import ru.rgasymov.moneymanager.service.HistoryService;
import ru.rgasymov.moneymanager.service.SavingService;
import ru.rgasymov.moneymanager.service.UserService;

@Service
@Slf4j
public class ExpenseService
    extends BaseOperationService<Expense, ExpenseCategory> {

  private final ExpenseRepository expenseRepository;

  private final SavingService savingService;

  private final ExpenseMapper expenseMapper;

  private final UserService userService;

  private final HistoryService historyService;

  public ExpenseService(
      ExpenseRepository expenseRepository,
      ExpenseCategoryRepository expenseCategoryRepository,
      ExpenseMapper expenseMapper,
      UserService userService,
      SavingService savingService,
      HistoryService historyService) {
    super(expenseRepository, expenseCategoryRepository, expenseMapper, userService);
    this.expenseRepository = expenseRepository;
    this.savingService = savingService;
    this.expenseMapper = expenseMapper;
    this.userService = userService;
    this.historyService = historyService;
  }

  @Override
  protected OperationResponseDto saveNewOperation(Expense operation) {
    var value = operation.getValue();
    var date = operation.getDate();
    savingService.decrease(value, date);
    var saving = savingService.findByDate(date);
    operation.setSavingId(saving.getId());

    var saved = expenseRepository.save(operation);
    return expenseMapper.toDto(saved);
  }

  @Override
  protected Expense buildNewOperation(OperationRequestDto dto,
                                      ExpenseCategory category) {
    var currentUser = userService.getCurrentUser();
    return Expense.builder()
        .date(dto.getDate())
        .value(dto.getValue())
        .description(dto.getDescription())
        .isPlanned(dto.getIsPlanned())
        .category(category)
        .accountId(currentUser.getCurrentAccount().getId())
        .build();
  }

  @Override
  protected Expense cloneOperation(Expense operation) {
    return operation.clone();
  }

  @Override
  protected void updateSavings(BigDecimal value,
                               BigDecimal oldValue,
                               LocalDate date,
                               Expense operation) {
    var subtract = value.subtract(oldValue);
    if (subtract.signum() > 0) {
      savingService.decrease(subtract, date);
    } else {
      savingService.increase(subtract.abs(), date);
    }
    var saving = savingService.findByDate(date);
    operation.setSavingId(saving.getId());
    operation.setValue(value);
  }

  @Override
  protected void deleteOperation(Expense operation, Long currentAccountId) {
    expenseRepository.deleteByIdAndAccountId(operation.getId(), currentAccountId);
    savingService.increase(operation.getValue(), operation.getDate());
    savingService.updateAfterDeletionOperation(operation.getDate());
  }

  @Override
  protected void logCreate(OperationResponseDto operation) {
    historyService.logCreate(operation, OperationType.EXPENSE);
  }

  @Override
  protected void logUpdate(Expense oldOperation, OperationResponseDto newOperation) {
    var oldDto = expenseMapper.toDto(oldOperation);
    historyService.logUpdate(oldDto, newOperation, OperationType.EXPENSE);
  }

  @Override
  protected void logDelete(Expense operation) {
    var oldDto = expenseMapper.toDto(operation);
    historyService.logDelete(oldDto, OperationType.EXPENSE);
  }
}
