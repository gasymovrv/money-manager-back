package ru.rgasymov.moneymanager.security.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import ru.rgasymov.moneymanager.exception.NotAllowedRedirectUriException;
import ru.rgasymov.moneymanager.security.TokenProvider;
import ru.rgasymov.moneymanager.util.CookieUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class Oauth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final TokenProvider tokenProvider;
  private final HttpCookieOauth2AuthorizationRequestRepository
      httpCookieOauth2AuthorizationRequestRepository;
  @Value("${security.allowed-origins}")
  private List<String> authorizedRedirectHosts;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException {
    String targetUrl;
    try {
      targetUrl = determineTargetUrl(request, response, authentication);
    } catch (NotAllowedRedirectUriException e) {
      response.sendError(HttpStatus.FORBIDDEN.value(), e.getMessage());
      return;
    }

    if (response.isCommitted()) {
      log.debug("Response has already been committed. Unable to redirect to " + targetUrl);
      return;
    }

    clearAuthenticationAttributes(request, response);
    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }

  protected String determineTargetUrl(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Authentication authentication) {
    var redirectUri = CookieUtils.getCookie(request,
            HttpCookieOauth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
        .map(Cookie::getValue);

    if (redirectUri.isPresent() && !isAuthorizedRedirectUri(redirectUri.get())) {
      throw new NotAllowedRedirectUriException(
          "Got an Unauthorized Redirect URI and can't proceed with the authentication");
    }
    var token = tokenProvider.createToken(authentication);

    return UriComponentsBuilder.fromUriString(redirectUri.orElse(getDefaultTargetUrl()))
        .queryParam("token", token)
        .build().toUriString();
  }

  protected void clearAuthenticationAttributes(HttpServletRequest request,
                                               HttpServletResponse response) {
    super.clearAuthenticationAttributes(request);
    httpCookieOauth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request,
        response);
  }

  private boolean isAuthorizedRedirectUri(String uri) {
    var clientRedirectUri = URI.create(uri);

    return authorizedRedirectHosts
        .stream()
        .anyMatch(authorizedRedirectUri -> {
          // Only validate host and port. Let the clients use different paths if they want to
          var authorizedUri = URI.create(authorizedRedirectUri);
          return authorizedUri.getHost().equalsIgnoreCase(clientRedirectUri.getHost())
              && authorizedUri.getPort() == clientRedirectUri.getPort();
        });
  }
}
