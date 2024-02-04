package ru.rgasymov.moneymanager.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rgasymov.moneymanager.domain.dto.response.HistoryActionDto;
import ru.rgasymov.moneymanager.domain.dto.response.OperationResponseDto;
import ru.rgasymov.moneymanager.domain.entity.HistoryAction;
import ru.rgasymov.moneymanager.domain.enums.HistoryActionType;
import ru.rgasymov.moneymanager.domain.enums.OperationType;
import ru.rgasymov.moneymanager.mapper.HistoryMapper;
import ru.rgasymov.moneymanager.repository.HistoryRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryService {

  private final HistoryRepository historyRepository;

  private final HistoryMapper historyMapper;

  private final UserService userService;

  @Transactional(readOnly = true)
  public List<HistoryActionDto> findAll() {
    var list = historyRepository.findAll();
    return historyMapper.toDtos(list);
  }

  @Transactional
  public void logCreate(OperationResponseDto newOperation, OperationType operationType) {
    var currentUser = userService.getCurrentUser();
    var currentAccount = currentUser.getCurrentAccount();
    var now = LocalDate.now();
    newOperation.calculateOverdue(now);

    historyRepository.save(
        HistoryAction
            .builder()
            .account(currentAccount)
            .operationType(operationType)
            .actionType(HistoryActionType.CREATE)
            .newOperation(newOperation)
            .build()
    );
  }

  @Transactional
  public void logUpdate(OperationResponseDto oldOperation,
                        OperationResponseDto newOperation,
                        OperationType operationType) {
    var currentUser = userService.getCurrentUser();
    var currentAccount = currentUser.getCurrentAccount();
    var now = LocalDate.now();
    oldOperation.calculateOverdue(now);
    newOperation.calculateOverdue(now);

    historyRepository.save(
        HistoryAction
            .builder()
            .account(currentAccount)
            .operationType(operationType)
            .actionType(HistoryActionType.UPDATE)
            .newOperation(newOperation)
            .oldOperation(oldOperation)
            .build()
    );
  }

  @Transactional
  public void logDelete(OperationResponseDto oldOperation,
                        OperationType operationType) {
    var currentUser = userService.getCurrentUser();
    var currentAccount = currentUser.getCurrentAccount();
    var now = LocalDate.now();
    oldOperation.calculateOverdue(now);

    historyRepository.save(
        HistoryAction
            .builder()
            .account(currentAccount)
            .operationType(operationType)
            .actionType(HistoryActionType.DELETE)
            .oldOperation(oldOperation)
            .build()
    );
  }
}
