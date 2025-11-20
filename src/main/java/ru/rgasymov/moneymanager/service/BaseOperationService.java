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

  /**
   * Updates an existing operation (Income or Expense) with new data.
   *
   * <p>This method handles two distinct update scenarios:
   * <ol>
   *   <li><b>Date change:</b> When the operation date changes, the old operation is deleted
   *       and a new one is created. This is necessary because changing the date affects
   *       the associated {@code Saving} entity and requires recalculating savings for
   *       multiple dates.</li>
   *   <li><b>Same date update:</b> When only non-date fields change (value, description, etc.),
   *       the operation is updated in place.</li>
   * </ol>
   *
   * <p><b>Critical implementation detail - Avoiding optimistic locking errors:</b>
   *
   * <p>The {@code updatedOperation} is built as a new transient entity without an ID.
   * The ID is deliberately set AFTER the date change check, not before. This ordering
   * is crucial:
   *
   * <ul>
   *   <li><b>For date changes:</b> The entity remains transient (no ID) when passed to
   *       {@code saveNewOperation()}, allowing Hibernate to perform a proper INSERT for the
   *       new operation.</li>
   *   <li><b>For same-date updates:</b> The ID is set just before {@code save()}, making it
   *       a detached entity with an ID. When {@code save()} is called, Hibernate performs
   *       an UPDATE using the ID.</li>
   * </ul>
   *
   * <p>Setting the ID earlier (before the date check) would cause the entity to have an ID
   * when passed to {@code saveNewOperation()} for date changes, leading to Hibernate attempting
   * a merge operation. This can trigger optimistic locking exceptions because the entity is
   * detached and lacks proper version tracking.
   *
   * @param id the ID of the operation to update
   * @param dto the new operation data from the request
   * @return the updated operation as a DTO
   * @throws EntityNotFoundException if the operation or category is not found
   */
  @Transactional
  @Override
  public OperationResponseDto update(Long id, OperationRequestDto dto) {
    var currentUser = userService.getCurrentUser();
    var currentAccountId = currentUser.getCurrentAccount().getId();
    var categoryId = dto.getCategoryId();

    // Clone the existing operation to preserve original values for logging
    // (after save, Hibernate may modify the entity in place)
    final O oldOperation = cloneOperation(
        operationRepository.findByIdAndAccountId(id, currentAccountId)
            .orElseThrow(() ->
                new EntityNotFoundException(
                    String.format("Could not find operation with id = '%s' in the database",
                        id)))
    );
    C category = findCategory(categoryId, currentAccountId);

    // Build a new transient entity (no ID yet) with the updated data
    O updatedOperation = buildNewOperation(dto, category);

    var oldDate = oldOperation.getDate();
    var date = updatedOperation.getDate();
    
    // Handle date change: delete old and create new
    // Note: updatedOperation has no ID, so saveNewOperation() will INSERT
    if (isChanged(oldDate, date)) {
      deleteOperation(oldOperation, currentAccountId);
      var savedDto = saveNewOperation(updatedOperation);
      logUpdate(oldOperation, savedDto);
      return savedDto;
    }

    // CRITICAL: Set ID only after date check to ensure proper Hibernate operation
    // This makes the entity detached with an ID, allowing UPDATE on save()
    updatedOperation.setId(id);
    
    var oldValue = oldOperation.getValue();
    var value = updatedOperation.getValue();
    if (isChanged(oldValue, value)) {
      // Update savings and set the new savingId on the operation
      updateSavings(value, oldValue, date, updatedOperation);
    } else {
      // Value unchanged, preserve the existing savingId
      updatedOperation.setSavingId(oldOperation.getSavingId());
    }

    // Hibernate will perform UPDATE because updatedOperation has an ID
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
