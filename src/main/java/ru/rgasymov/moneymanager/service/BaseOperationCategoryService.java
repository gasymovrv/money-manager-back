package ru.rgasymov.moneymanager.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import ru.rgasymov.moneymanager.domain.dto.request.OperationCategoryRequestDto;
import ru.rgasymov.moneymanager.domain.dto.response.OperationCategoryResponseDto;
import ru.rgasymov.moneymanager.domain.entity.BaseOperation;
import ru.rgasymov.moneymanager.domain.entity.BaseOperationCategory;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.mapper.BaseOperationCategoryMapper;
import ru.rgasymov.moneymanager.repository.BaseOperationCategoryRepository;
import ru.rgasymov.moneymanager.repository.BaseOperationRepository;

@RequiredArgsConstructor
public abstract class BaseOperationCategoryService<
    O extends BaseOperation,
    C extends BaseOperationCategory> {

  private final BaseOperationRepository<O> operationRepository;

  private final BaseOperationCategoryRepository<C> operationCategoryRepository;

  private final BaseOperationCategoryMapper<C> operationCategoryMapper;

  private final UserService userService;

  protected abstract C buildNewOperationCategory(User currentUser, String name);

  protected abstract void clearCachedCategories();

  protected abstract List<OperationCategoryResponseDto> findAll(Long accountId);

  protected abstract List<OperationCategoryResponseDto> findAllAndSetChecked(Long accountId,
                                                                             List<Long> ids);

  @Transactional
  public OperationCategoryResponseDto create(OperationCategoryRequestDto dto) {
    var currentUser = userService.getCurrentUser();
    var currentAccountId = currentUser.getCurrentAccount().getId();

    if (operationCategoryRepository
        .existsByNameIgnoreCaseAndAccountId(dto.getName(), currentAccountId)) {
      throw new ValidationException(
          "Could not create operation category because such name already exists");
    }

    C newOperationCategory = buildNewOperationCategory(currentUser, dto.getName());
    C saved = operationCategoryRepository.save(newOperationCategory);

    clearCachedCategories();
    return operationCategoryMapper.toDto(saved);
  }

  @Transactional
  public OperationCategoryResponseDto update(Long id, OperationCategoryRequestDto dto) {
    var currentUser = userService.getCurrentUser();
    var currentAccountId = currentUser.getCurrentAccount().getId();

    if (operationCategoryRepository
        .existsByNameIgnoreCaseAndAccountId(dto.getName(), currentAccountId)) {
      throw new ValidationException(
          "Could not update operation category because such name already exists");
    }

    C operationCategory = operationCategoryRepository.findByIdAndAccountId(id, currentAccountId)
        .orElseThrow(() ->
            new EntityNotFoundException(
                String.format("Could not find operation category with id = '%s' in the database",
                    id)));
    operationCategory.setName(dto.getName());
    C saved = operationCategoryRepository.save(operationCategory);

    clearCachedCategories();
    return operationCategoryMapper.toDto(saved);
  }

  @Transactional
  public void delete(Long id) {
    var currentUser = userService.getCurrentUser();
    var currentAccountId = currentUser.getCurrentAccount().getId();

    if (operationRepository.existsByCategoryId(id)) {
      throw new ValidationException(
          "Could not delete an operation category while it is being referenced by any expenses");
    }
    operationCategoryRepository.deleteByIdAndAccountId(id, currentAccountId);
    clearCachedCategories();
  }
}
