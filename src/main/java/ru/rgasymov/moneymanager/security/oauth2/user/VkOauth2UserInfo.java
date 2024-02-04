package ru.rgasymov.moneymanager.security.oauth2.user;

import java.util.Map;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;

public class VkOauth2UserInfo extends Oauth2UserInfo {

  public VkOauth2UserInfo(Map<String, Object> attributes, AuthProviders provider) {
    super(attributes, provider);
  }

  @Override
  public String getId() {
    return String.valueOf(attributes.get("id"));
  }

  @Override
  public String getName() {
    var firstName = (String) attributes.get("first_name");
    var lastName = (String) attributes.get("last_name");
    return String.format("%s %s", firstName, lastName);
  }

  @Override
  public String getEmail() {
    return (String) attributes.get("email");
  }

  @Override
  public String getImageUrl() {
    return (String) attributes.get("photo_max");
  }

  @Override
  public String getLocale() {
    return null;
  }
}
