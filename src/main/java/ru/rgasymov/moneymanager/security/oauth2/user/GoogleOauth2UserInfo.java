package ru.rgasymov.moneymanager.security.oauth2.user;

import java.util.Map;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;

public class GoogleOauth2UserInfo extends Oauth2UserInfo {

  public GoogleOauth2UserInfo(Map<String, Object> attributes, AuthProviders provider) {
    super(attributes, provider);
  }

  @Override
  public String getId() {
    return (String) attributes.get("sub");
  }

  @Override
  public String getName() {
    return (String) attributes.get("name");
  }

  @Override
  public String getEmail() {
    return (String) attributes.get("email");
  }

  @Override
  public String getImageUrl() {
    return (String) attributes.get("picture");
  }

  @Override
  public String getLocale() {
    return (String) attributes.get("locale");
  }
}
