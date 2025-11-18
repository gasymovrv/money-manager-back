package ru.rgasymov.moneymanager.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.AuthenticationException;

class RestAuthenticationEntryPointTest {

  @Test
  void commence_shouldSendUnauthorizedError() throws IOException {
    var entryPoint = new RestAuthenticationEntryPoint();
    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    var authException = mock(AuthenticationException.class);
    var errorMessage = "Authentication failed";
    org.mockito.Mockito.when(authException.getLocalizedMessage()).thenReturn(errorMessage);

    entryPoint.commence(request, response, authException);

    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, errorMessage);
  }

  @Test
  void commence_shouldHandleNullMessage() throws IOException {
    var entryPoint = new RestAuthenticationEntryPoint();
    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    var authException = mock(AuthenticationException.class);
    org.mockito.Mockito.when(authException.getLocalizedMessage()).thenReturn(null);

    entryPoint.commence(request, response, authException);

    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, null);
  }
}
