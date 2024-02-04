package ru.rgasymov.moneymanager.security.oauth2;

import static ru.rgasymov.moneymanager.util.TokenUtils.getExpiresIn;
import static ru.rgasymov.moneymanager.util.TokenUtils.getParameterValue;
import static ru.rgasymov.moneymanager.util.TokenUtils.getScopes;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

public class CustomTokenResponseConverter
    implements Converter<Map<String, Object>, OAuth2AccessTokenResponse> {

  private static final Set<String> TOKEN_RESPONSE_PARAMETER_NAMES = new HashSet<>(
      List.of(OAuth2ParameterNames.ACCESS_TOKEN,
          OAuth2ParameterNames.EXPIRES_IN,
          OAuth2ParameterNames.REFRESH_TOKEN,
          OAuth2ParameterNames.SCOPE,
          OAuth2ParameterNames.TOKEN_TYPE));

  @Override
  public OAuth2AccessTokenResponse convert(Map<String, Object> source) {
    var accessToken = getParameterValue(source, OAuth2ParameterNames.ACCESS_TOKEN);
    var accessTokenType = OAuth2AccessToken.TokenType.BEARER;
    var expiresIn = getExpiresIn(source);
    var scopes = getScopes(source);
    var refreshToken = getParameterValue(source, OAuth2ParameterNames.REFRESH_TOKEN);

    Map<String, Object> additionalParameters = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : source.entrySet()) {
      if (!TOKEN_RESPONSE_PARAMETER_NAMES.contains(entry.getKey())) {
        additionalParameters.put(entry.getKey(), entry.getValue());
      }
    }
    return OAuth2AccessTokenResponse.withToken(accessToken)
        .tokenType(accessTokenType)
        .expiresIn(expiresIn)
        .scopes(scopes)
        .refreshToken(refreshToken)
        .additionalParameters(additionalParameters)
        .build();
  }
}
