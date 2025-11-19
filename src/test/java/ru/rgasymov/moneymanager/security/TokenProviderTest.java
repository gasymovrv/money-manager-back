package ru.rgasymov.moneymanager.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtException;
import ru.rgasymov.moneymanager.domain.entity.Account;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;

class TokenProviderTest {

  private TokenProvider tokenProvider;
  private static final String TEST_SECRET = "testSecretKeyThatIsLongEnoughForHS256Algorithm";
  private static final Duration EXPIRATION_PERIOD = Duration.ofHours(1);

  @BeforeEach
  void setUp() {
    tokenProvider = new TokenProvider(TEST_SECRET, EXPIRATION_PERIOD);
  }

  @Test
  void createToken_shouldGenerateValidToken() {
    var user = createTestUser();
    var userPrincipal = UserPrincipal.create(user);
    var authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(userPrincipal);

    var token = tokenProvider.createToken(authentication);

    assertThat(token).isNotNull();
    assertThat(token).isNotEmpty();
  }

  @Test
  void parseToken_shouldParseValidToken() {
    var user = createTestUser();
    var userPrincipal = UserPrincipal.create(user);
    var authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(userPrincipal);
    var token = tokenProvider.createToken(authentication);

    var jwt = tokenProvider.parseToken(token);

    assertThat(jwt).isNotNull();
  }

  @Test
  void getUserIdFromToken_shouldExtractUserId() {
    var user = createTestUser();
    var userPrincipal = UserPrincipal.create(user);
    var authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(userPrincipal);
    var token = tokenProvider.createToken(authentication);
    var jwt = tokenProvider.parseToken(token);

    var userId = tokenProvider.getUserIdFromToken(jwt);

    assertThat(userId).isEqualTo("user123");
  }

  @Test
  void parseToken_shouldThrowException_whenTokenIsInvalid() {
    var invalidToken = "invalid.token.here";

    assertThatThrownBy(() -> tokenProvider.parseToken(invalidToken))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void parseToken_shouldThrowException_whenTokenIsMalformed() {
    var malformedToken = "not-a-jwt-token";

    assertThatThrownBy(() -> tokenProvider.parseToken(malformedToken))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void createToken_shouldIncludeExpirationTime() {
    var user = createTestUser();
    var userPrincipal = UserPrincipal.create(user);
    var authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(userPrincipal);
    var token = tokenProvider.createToken(authentication);
    var jwt = tokenProvider.parseToken(token);

    assertThat(jwt.getExpiresAt()).isNotNull();
    assertThat(jwt.getIssuedAt()).isNotNull();
  }

  @Test
  void parseToken_shouldThrowException_whenTokenIsEmpty() {
    var emptyToken = "";

    assertThatThrownBy(() -> tokenProvider.parseToken(emptyToken))
        .isInstanceOf(JwtException.class);
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
}
