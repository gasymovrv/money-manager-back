package ru.rgasymov.moneymanager.security.oauth2;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import ru.rgasymov.moneymanager.util.CookieUtils;

/**
 * By default, Spring OAuth2 uses HttpSessionOAuth2AuthorizationRequestRepository to save
 * the authorization request. But, since our service is stateless, we can't save it in
 * the session. We'll save the request in a Base64 encoded cookie instead.
 **/
@Component
public class HttpCookieOauth2AuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
  public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
  public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
  private static final int authCookieExpireSeconds = 180;

  /**
   * Spring invokes it on redirectionEndpoint.
   * (/oauth2/callback/*, see {@link ru.rgasymov.moneymanager.config.SecurityConfig#securityFilterChain})
   * with built {@link OAuth2AuthorizationRequest}
   *
   * @param request http request
   * @return {@link OAuth2AuthorizationRequest} which was retrieved from cookie saved earlier by {@link #saveAuthorizationRequest}
   */
  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                               HttpServletResponse response) {
    return this.loadAuthorizationRequest(request);
  }

  /**
   * Spring invokes it on authorizationEndpoint.
   * (/oauth2/authorize/PROVIDER, see {@link ru.rgasymov.moneymanager.config.SecurityConfig#securityFilterChain})
   * with built {@link OAuth2AuthorizationRequest}
   * Method saves OAuth2AuthorizationRequest in a cookie before redirect to the provider.
   *
   * @param authorizationRequest auth request that will be sent to the provider
   * @param request              http request
   * @param response             http response
   */
  @Override
  public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
    if (authorizationRequest == null) {
      CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
      CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
      return;
    }

    CookieUtils.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
        CookieUtils.serialize(authorizationRequest), authCookieExpireSeconds);
    String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
    if (StringUtils.isNotBlank(redirectUriAfterLogin)) {
      CookieUtils.addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin);
    }
  }

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
        .map(cookie -> CookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class))
        .orElse(null);
  }

  public void removeAuthorizationRequestCookies(HttpServletRequest request,
                                                HttpServletResponse response) {
    CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
    CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
  }
}
