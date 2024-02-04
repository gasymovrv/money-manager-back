package ru.rgasymov.moneymanager.security.oauth2.user;

import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;

@RequiredArgsConstructor
@Getter
public abstract class Oauth2UserInfo {

  protected final Map<String, Object> attributes;
  protected final AuthProviders provider;

  public abstract String getId();

  public abstract String getName();

  public abstract String getEmail();

  public abstract String getImageUrl();

  public abstract String getLocale();
}
