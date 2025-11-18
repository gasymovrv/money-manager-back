package ru.rgasymov.moneymanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.rgasymov.moneymanager.domain.dto.response.HistoryActionDto;
import ru.rgasymov.moneymanager.domain.dto.response.OperationResponseDto;
import ru.rgasymov.moneymanager.domain.entity.Account;
import ru.rgasymov.moneymanager.domain.entity.HistoryAction;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;
import ru.rgasymov.moneymanager.domain.enums.OperationType;
import ru.rgasymov.moneymanager.mapper.HistoryMapper;
import ru.rgasymov.moneymanager.repository.HistoryRepository;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

  @Mock
  private HistoryRepository historyRepository;

  @Mock
  private HistoryMapper historyMapper;

  @Mock
  private UserService userService;

  private HistoryService historyService;

  @BeforeEach
  void setUp() {
    historyService = new HistoryService(historyRepository, historyMapper, userService);
  }

  @Test
  void findAll_shouldReturnPagedHistoryActions() {
    var user = createTestUser();
    var historyAction = new HistoryAction();
    var dto = new HistoryActionDto();
    var pageable = PageRequest.of(0, 10);

    when(userService.getCurrentUser()).thenReturn(user);
    when(historyRepository.findAllByAccountIdOrderByModifiedAtDesc(eq(1L), any()))
        .thenReturn(new PageImpl<>(List.of(historyAction), pageable, 1));
    when(historyMapper.toDtos(any())).thenReturn(List.of(dto));

    var result = historyService.findAll(pageable);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getTotalElements()).isEqualTo(1);
    verify(historyRepository).findAllByAccountIdOrderByModifiedAtDesc(eq(1L), any());
  }

  @Test
  void logCreate_shouldSaveCreateHistoryAction() {
    var user = createTestUser();
    var operationDto = createOperationDto();

    when(userService.getCurrentUser()).thenReturn(user);

    historyService.logCreate(operationDto, OperationType.EXPENSE);

    verify(historyRepository).save(any(HistoryAction.class));
  }

  @Test
  void logUpdate_shouldSaveUpdateHistoryAction() {
    var user = createTestUser();
    var oldOperation = createOperationDto();
    var newOperation = createOperationDto();
    newOperation.setValue(BigDecimal.valueOf(200));

    when(userService.getCurrentUser()).thenReturn(user);

    historyService.logUpdate(oldOperation, newOperation, OperationType.EXPENSE);

    verify(historyRepository).save(any(HistoryAction.class));
  }

  @Test
  void logDelete_shouldSaveDeleteHistoryAction() {
    var user = createTestUser();
    var operationDto = createOperationDto();

    when(userService.getCurrentUser()).thenReturn(user);

    historyService.logDelete(operationDto, OperationType.INCOME);

    verify(historyRepository).save(any(HistoryAction.class));
  }

  private User createTestUser() {
    var account = Account.builder()
        .id(1L)
        .name("Test Account")
        .theme(AccountTheme.LIGHT)
        .currency("USD")
        .build();

    return User.builder()
        .id("user123")
        .email("test@example.com")
        .name("Test User")
        .provider(AuthProviders.GOOGLE)
        .currentAccount(account)
        .build();
  }

  private OperationResponseDto createOperationDto() {
    var dto = new OperationResponseDto();
    dto.setId(1L);
    dto.setDate(LocalDate.now());
    dto.setValue(BigDecimal.valueOf(100));
    dto.setDescription("Test operation");
    dto.setPlanned(false);
    return dto;
  }
}
