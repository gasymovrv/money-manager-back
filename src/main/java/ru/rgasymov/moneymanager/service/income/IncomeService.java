package ru.rgasymov.moneymanager.service.income;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.rgasymov.moneymanager.domain.dto.request.OperationRequestDto;
import ru.rgasymov.moneymanager.domain.dto.response.OperationResponseDto;
import ru.rgasymov.moneymanager.domain.entity.Income;
import ru.rgasymov.moneymanager.domain.entity.IncomeCategory;
import ru.rgasymov.moneymanager.domain.enums.OperationType;
import ru.rgasymov.moneymanager.mapper.IncomeMapper;
import ru.rgasymov.moneymanager.repository.IncomeCategoryRepository;
import ru.rgasymov.moneymanager.repository.IncomeRepository;
import ru.rgasymov.moneymanager.service.BaseOperationService;
import ru.rgasymov.moneymanager.service.HistoryService;
import ru.rgasymov.moneymanager.service.SavingService;
import ru.rgasymov.moneymanager.service.UserService;

@Service
@Slf4j
public class IncomeService
    extends BaseOperationService<Income, IncomeCategory> {

  private final IncomeRepository incomeRepository;

  private final SavingService savingService;

  private final IncomeMapper incomeMapper;

  private final UserService userService;

  private final HistoryService historyService;

  public IncomeService(
      IncomeRepository incomeRepository,
      IncomeCategoryRepository incomeCategoryRepository,
      IncomeMapper incomeMapper,
      UserService userService,
      SavingService savingService, HistoryService historyService) {
    super(incomeRepository, incomeCategoryRepository, incomeMapper, userService);
    this.incomeRepository = incomeRepository;
    this.savingService = savingService;
    this.incomeMapper = incomeMapper;
    this.userService = userService;
    this.historyService = historyService;
  }

  @Override
  protected OperationResponseDto saveNewOperation(Income operation) {
    var value = operation.getValue();
    var date = operation.getDate();
    savingService.increase(value, date);
    var saving = savingService.findByDate(date);
    operation.setSavingId(saving.getId());

    var saved = incomeRepository.save(operation);
    return incomeMapper.toDto(saved);
  }

  @Override
  protected Income buildNewOperation(OperationRequestDto dto,
                                     IncomeCategory category) {
    var currentUser = userService.getCurrentUser();
    return Income.builder()
        .date(dto.getDate())
        .value(dto.getValue())
        .description(dto.getDescription())
        .isPlanned(dto.getIsPlanned())
        .accountId(currentUser.getCurrentAccount().getId())
        .category(category)
        .build();
  }

  @Override
  protected Income cloneOperation(Income operation) {
    return operation.clone();
  }

  @Override
  protected void updateSavings(BigDecimal value,
                               BigDecimal oldValue,
                               LocalDate date,
                               Income operation) {
    var subtract = value.subtract(oldValue);
    if (subtract.signum() > 0) {
      savingService.increase(subtract, date);
    } else {
      savingService.decrease(subtract.abs(), date);
    }
    var saving = savingService.findByDate(date);
    operation.setSavingId(saving.getId());
    operation.setValue(value);
  }

  @Override
  protected void deleteOperation(Income operation, Long currentAccountId) {
    incomeRepository.deleteByIdAndAccountId(operation.getId(), currentAccountId);
    savingService.decrease(operation.getValue(), operation.getDate());
    savingService.updateAfterDeletionOperation(operation.getDate());
  }

  @Override
  protected void logCreate(OperationResponseDto operation) {
    historyService.logCreate(operation, OperationType.INCOME);
  }

  @Override
  protected void logUpdate(Income oldOperation, OperationResponseDto newOperation) {
    var oldDto = incomeMapper.toDto(oldOperation);
    historyService.logUpdate(oldDto, newOperation, OperationType.INCOME);
  }

  @Override
  protected void logDelete(Income operation) {
    var oldDto = incomeMapper.toDto(operation);
    historyService.logDelete(oldDto, OperationType.INCOME);
  }
}
