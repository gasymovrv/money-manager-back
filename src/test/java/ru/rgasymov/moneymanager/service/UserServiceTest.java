package ru.rgasymov.moneymanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.rgasymov.moneymanager.domain.entity.Account;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;
import ru.rgasymov.moneymanager.exception.UserNotFoundException;
import ru.rgasymov.moneymanager.mapper.UserMapper;
import ru.rgasymov.moneymanager.repository.ExpenseCategoryRepository;
import ru.rgasymov.moneymanager.repository.IncomeCategoryRepository;
import ru.rgasymov.moneymanager.repository.UserRepository;
import ru.rgasymov.moneymanager.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private IncomeCategoryRepository incomeCategoryRepository;

  @Mock
  private ExpenseCategoryRepository expenseCategoryRepository;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private Authentication authentication;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(
        userRepository,
        userMapper,
        incomeCategoryRepository,
        expenseCategoryRepository
    );
    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  void loadUserByIdAsUserDetails_shouldReturnUserPrincipal_whenUserExists() {
    var user = createTestUser();
    when(userRepository.findById("user123")).thenReturn(Optional.of(user));

    var userDetails = userService.loadUserByIdAsUserDetails("user123");

    assertThat(userDetails).isNotNull();
    assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
    verify(userRepository).findById("user123");
  }

  @Test
  void loadUserByIdAsUserDetails_shouldThrowException_whenUserNotFound() {
    when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.loadUserByIdAsUserDetails("nonexistent"))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("nonexistent");
  }

  @Test
  void getCurrentUser_shouldReturnCurrentUser() {
    var user = createTestUser();
    var userPrincipal = UserPrincipal.create(user);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(userPrincipal);

    var currentUser = userService.getCurrentUser();

    assertThat(currentUser).isNotNull();
    assertThat(currentUser.getId()).isEqualTo("user123");
    assertThat(currentUser.getEmail()).isEqualTo("test@example.com");
  }

  @Test
  void getCurrentUserAsDto_shouldReturnUserDto_withDraftFlag() {
    var user = createTestUser();
    var userPrincipal = UserPrincipal.create(user);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(userPrincipal);
    when(incomeCategoryRepository.existsByAccountId(1L)).thenReturn(false);
    when(expenseCategoryRepository.existsByAccountId(1L)).thenReturn(false);

    // Mock the mapper and its result
    var mockDto = org.mockito.Mockito.mock(
        ru.rgasymov.moneymanager.domain.dto.response.UserResponseDto.class);
    var mockAccountDto = org.mockito.Mockito.mock(
        ru.rgasymov.moneymanager.domain.dto.response.AccountResponseDto.class);
    when(userMapper.toDto(user)).thenReturn(mockDto);
    when(mockDto.getCurrentAccount()).thenReturn(mockAccountDto);

    var result = userService.getCurrentUserAsDto();

    assertThat(result).isNotNull();
    verify(mockAccountDto).setDraft(true);
  }

  @Test
  void findById_shouldReturnUser_whenExists() {
    var user = createTestUser();
    when(userRepository.findById("user123")).thenReturn(Optional.of(user));

    var result = userService.findById("user123");

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo("user123");
  }

  @Test
  void findById_shouldReturnEmpty_whenNotExists() {
    when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

    var result = userService.findById("nonexistent");

    assertThat(result).isEmpty();
  }

  @Test
  void save_shouldSaveUser() {
    var user = createTestUser();
    when(userRepository.save(user)).thenReturn(user);

    var result = userService.save(user);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo("user123");
    verify(userRepository).save(user);
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
        .locale("en")
        .picture("http://example.com/picture.jpg")
        .lastVisit(LocalDateTime.now())
        .currentAccount(account)
        .build();
  }
}
