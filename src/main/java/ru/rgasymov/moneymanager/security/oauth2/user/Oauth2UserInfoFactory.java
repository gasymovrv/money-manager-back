package ru.rgasymov.moneymanager.security.oauth2.user;

import java.util.Map;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;
import ru.rgasymov.moneymanager.exception.Oauth2AuthenticationProcessingException;

public class Oauth2UserInfoFactory {

  public static Oauth2UserInfo getOauth2UserInfo(AuthProviders provider,
                                                 Map<String, Object> attributes) {
    if (AuthProviders.GOOGLE == provider) {
      return new GoogleOauth2UserInfo(attributes, AuthProviders.GOOGLE);
    } else if (AuthProviders.VK == provider) {
      return new VkOauth2UserInfo(attributes, AuthProviders.VK);
    } else {
      throw new Oauth2AuthenticationProcessingException(
          String.format("Login with '%s' is not supported yet.", provider));
    }
  }
}
