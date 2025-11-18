package ru.rgasymov.moneymanager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.ClassRule;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.rgasymov.moneymanager.PostgresContainerInitializer;
import ru.rgasymov.moneymanager.domain.entity.Account;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;
import ru.rgasymov.moneymanager.repository.AccountRepository;
import ru.rgasymov.moneymanager.repository.UserRepository;
import ru.rgasymov.moneymanager.security.TokenProvider;
import ru.rgasymov.moneymanager.security.UserPrincipal;

/**
 * Base class for integration tests with TestContainers.
 * Provides common setup and utilities for integration testing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

  @ClassRule
  public static final PostgreSQLContainer<?> postgreSQLContainer =
      PostgresContainerInitializer.initialize();

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected UserRepository userRepository;

  @Autowired
  protected AccountRepository accountRepository;

  @Autowired
  protected TokenProvider tokenProvider;

  protected User testUser;
  protected Account testAccount;
  protected String authToken;

  @BeforeEach
  void baseSetUp() {
    SecurityContextHolder.clearContext();
    createTestUserAndAccount();
  }

  /**
   * Creates a test user and account for integration tests.
   */
  protected void createTestUserAndAccount() {
    testUser = User.builder()
        .id("test-user-id-" + System.currentTimeMillis())
        .email("test@example.com")
        .name("Test User")
        .provider(AuthProviders.GOOGLE)
        .locale("en")
        .picture("http://example.com/picture.jpg")
        .lastVisit(LocalDateTime.now())
        .build();

    testAccount = Account.builder()
        .name("Test Account")
        .theme(AccountTheme.LIGHT)
        .currency("USD")
        .user(testUser)
        .build();

    testAccount = accountRepository.save(testAccount);
    testUser.setCurrentAccount(testAccount);
    testUser = userRepository.save(testUser);

    // Create authentication token
    var userPrincipal = UserPrincipal.create(testUser);
    var authentication = new UsernamePasswordAuthenticationToken(
        userPrincipal,
        null,
        userPrincipal.getAuthorities()
    );
    SecurityContextHolder.getContext().setAuthentication(authentication);
    authToken = tokenProvider.createToken(authentication);
  }

  /**
   * Returns the authorization header value for authenticated requests.
   */
  protected String getAuthorizationHeader() {
    return "Bearer " + authToken;
  }

  /**
   * Creates an additional test user with their own account.
   */
  protected User createAdditionalUser(String id, String email) {
    var user = User.builder()
        .id(id)
        .email(email)
        .name("Additional User")
        .provider(AuthProviders.GOOGLE)
        .locale("en")
        .lastVisit(LocalDateTime.now())
        .build();

    var account = Account.builder()
        .name("Additional Account")
        .theme(AccountTheme.LIGHT)
        .currency("USD")
        .user(user)
        .build();

    account = accountRepository.save(account);
    user.setCurrentAccount(account);
    return userRepository.save(user);
  }
}
