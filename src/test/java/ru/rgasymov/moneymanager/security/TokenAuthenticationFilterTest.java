package ru.rgasymov.moneymanager.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import ru.rgasymov.moneymanager.domain.entity.Account;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;
import ru.rgasymov.moneymanager.exception.UserNotFoundException;
import ru.rgasymov.moneymanager.service.UserService;

@ExtendWith(MockitoExtension.class)
class TokenAuthenticationFilterTest {

  @Mock
  private TokenProvider tokenProvider;

  @Mock
  private UserService userService;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  private TokenAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    filter = new TokenAuthenticationFilter(tokenProvider, userService);
  }

  @Test
  void doFilterInternal_shouldSetAuthentication_whenValidToken() throws Exception {
    var token = "valid-token";
    var jwt = mock(Jwt.class);
    var user = createTestUser();
    var userPrincipal = UserPrincipal.create(user);

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(tokenProvider.parseToken(token)).thenReturn(jwt);
    when(tokenProvider.getUserIdFromToken(jwt)).thenReturn("user123");
    when(userService.loadUserByIdAsUserDetails("user123")).thenReturn(userPrincipal);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_shouldNotSetAuthentication_whenNoAuthorizationHeader() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);

    filter.doFilterInternal(request, response, filterChain);

    verify(tokenProvider, never()).parseToken(anyString());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_shouldNotSetAuthentication_whenUserNotFound() throws Exception {
    var token = "valid-token";
    var jwt = mock(Jwt.class);

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(tokenProvider.parseToken(token)).thenReturn(jwt);
    when(tokenProvider.getUserIdFromToken(jwt)).thenReturn("user123");
    when(userService.loadUserByIdAsUserDetails("user123"))
        .thenThrow(new UserNotFoundException("user123"));

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_shouldContinueFilterChain_whenExceptionOccurs() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
    when(tokenProvider.parseToken(any())).thenThrow(new RuntimeException("Invalid token"));

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
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
