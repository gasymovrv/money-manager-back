package ru.rgasymov.moneymanager.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

@Slf4j
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private static final String LOGIN_URL = "/login";

  private final String apiBaseUrl;

  @Override
  public void commence(HttpServletRequest httpServletRequest,
                       HttpServletResponse httpServletResponse,
                       AuthenticationException e) throws IOException {
    log.error("Responding with unauthorized error", e);
    if (httpServletRequest.getRequestURI().startsWith(apiBaseUrl + "/")) {
      httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          e.getLocalizedMessage());
    } else {
      httpServletResponse.sendRedirect(LOGIN_URL);
    }
  }
}
