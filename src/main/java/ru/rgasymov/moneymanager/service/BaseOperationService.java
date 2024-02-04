package ru.rgasymov.moneymanager.service;

import static ru.rgasymov.moneymanager.util.ComparingUtils.isChanged;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import ru.rgasymov.moneymanager.domain.dto.request.OperationRequestDto;
import ru.rgasymov.moneymanager.domain.dto.response.OperationResponseDto;
import ru.rgasymov.moneymanager.domain.entity.BaseOperation;
import ru.rgasymov.moneymanager.domain.entity.BaseOperationCategory;
import ru.rgasymov.moneymanager.mapper.BaseOperationMapper;
import ru.rgasymov.moneymanager.repository.BaseOperationCategoryRepository;
import ru.rgasymov.moneymanager.repository.BaseOperationRepository;

@RequiredArgsConstructor
public abstract class BaseOperationService<
    O extends BaseOperation,
    C extends BaseOperationCategory>
    implements OperationService<O> {

  private final BaseOperationRepository<O> operationRepository;

  private final BaseOperationCategoryRepository<C> operationCategoryRepository;

  private final BaseOperationMapper<O> operationMapper;

  private final UserService userService;

  @Transactional
  @Override
  public OperationResponseDto create(OperationRequestDto dto) {
    var currentUser = userService.getCurrentUser();
    var currentAccountId = currentUser.getCurrentAccount().getId();
    var categoryId = dto.getCategoryId();

    C category = findCategory(categoryId, currentAccountId);
    O operation = buildNewOperation(dto, category);

    var saved = saveNewOperation(operation);
    logCreate(saved);
    return saved;
  }

  @Transactional
  @Override
  public void create(O operation) {
    var saved = saveNewOperation(operation);
    logCreate(saved);
  }

  @Transactional
  @Override
  public OperationResponseDto update(Long id, OperationRequestDto dto) {
    var currentUser = userService.getCurrentUser();
    var currentAccountId = currentUser.getCurrentAccount().getId();
    var categoryId = dto.getCategoryId();

    // Clone is needed to avoid changing after save by Hibernate
    final O oldOperation = cloneOperation(
        operationRepository.findByIdAndAccountId(id, currentAccountId)
            .orElseThrow(() ->
                new EntityNotFoundException(
                    String.format("Could not find operation with id = '%s' in the database",
                        id)))
    );
    C category = findCategory(categoryId, currentAccountId);
    O updatedOperation = buildNewOperation(dto, category);
    updatedOperation.setId(id);

    var oldDate = oldOperation.getDate();
    var date = updatedOperation.getDate();
    if (isChanged(oldDate, date)) {
      deleteOperation(oldOperation, currentAccountId);
      var savedDto = saveNewOperation(updatedOperation);
      logUpdate(oldOperation, savedDto);
      return savedDto;
    }

    var oldValue = oldOperation.getValue();
    var value = updatedOperation.getValue();
    if (isChanged(oldValue, value)) {
      updateSavings(value, oldValue, date, updatedOperation);
    } else {
      updatedOperation.setSavingId(oldOperation.getSavingId());
    }

    O saved = operationRepository.save(updatedOperation);

    var savedDto = operationMapper.toDto(saved);
    logUpdate(oldOperation, savedDto);
    return savedDto;
  }

  @Transactional
  @Override
  public void delete(Long id) {
    var currentUser = userService.getCurrentUser();
    var currentAccountId = currentUser.getCurrentAccount().getId();

    O operation = operationRepository.findByIdAndAccountId(id, currentAccountId)
        .orElseThrow(() ->
            new EntityNotFoundException(
                String.format("Could not find operation with id = '%s' in the database",
                    id)));

    deleteOperation(operation, currentAccountId);
    logDelete(operation);
  }

  private C findCategory(Long categoryId, Long currentAccountId) {
    return operationCategoryRepository.findByIdAndAccountId(categoryId, currentAccountId)
        .orElseThrow(() ->
            new EntityNotFoundException(
                String.format("Could not find operation category with id = '%s' in the database",
                    categoryId)));
  }

  protected abstract OperationResponseDto saveNewOperation(O operation);

  protected abstract O buildNewOperation(OperationRequestDto dto,
                                         C category);

  protected abstract O cloneOperation(O operation);

  protected abstract void updateSavings(BigDecimal value,
                                        BigDecimal oldValue,
                                        LocalDate date,
                                        O operation);

  protected abstract void deleteOperation(O operation, Long currentAccountId);

  protected abstract void logCreate(OperationResponseDto operation);

  protected abstract void logUpdate(O oldOperation,
                                    OperationResponseDto newOperation);

  protected abstract void logDelete(O operation);
}
