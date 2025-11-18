package ru.rgasymov.moneymanager.security.oauth2.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;

class GoogleOauth2UserInfoTest {

  @Test
  void getId_shouldReturnSubAttribute() {
    var attributes = Map.<String, Object>of("sub", "google123");
    var userInfo = new GoogleOauth2UserInfo(attributes, AuthProviders.GOOGLE);

    var id = userInfo.getId();

    assertThat(id).isEqualTo("google123");
  }

  @Test
  void getName_shouldReturnNameAttribute() {
    var attributes = Map.<String, Object>of("name", "John Doe");
    var userInfo = new GoogleOauth2UserInfo(attributes, AuthProviders.GOOGLE);

    var name = userInfo.getName();

    assertThat(name).isEqualTo("John Doe");
  }

  @Test
  void getEmail_shouldReturnEmailAttribute() {
    var attributes = Map.<String, Object>of("email", "john@example.com");
    var userInfo = new GoogleOauth2UserInfo(attributes, AuthProviders.GOOGLE);

    var email = userInfo.getEmail();

    assertThat(email).isEqualTo("john@example.com");
  }

  @Test
  void getImageUrl_shouldReturnPictureAttribute() {
    var attributes = Map.<String, Object>of("picture", "http://example.com/pic.jpg");
    var userInfo = new GoogleOauth2UserInfo(attributes, AuthProviders.GOOGLE);

    var imageUrl = userInfo.getImageUrl();

    assertThat(imageUrl).isEqualTo("http://example.com/pic.jpg");
  }

  @Test
  void getLocale_shouldReturnLocaleAttribute() {
    var attributes = Map.<String, Object>of("locale", "en");
    var userInfo = new GoogleOauth2UserInfo(attributes, AuthProviders.GOOGLE);

    var locale = userInfo.getLocale();

    assertThat(locale).isEqualTo("en");
  }

  @Test
  void getAttributes_shouldReturnAllAttributes() {
    var attributes = Map.<String, Object>of(
        "sub", "google123",
        "name", "John Doe",
        "email", "john@example.com",
        "picture", "http://example.com/pic.jpg",
        "locale", "en"
    );
    var userInfo = new GoogleOauth2UserInfo(attributes, AuthProviders.GOOGLE);

    assertThat(userInfo.getAttributes()).isEqualTo(attributes);
  }

  @Test
  void getProvider_shouldReturnGoogle() {
    var attributes = Map.<String, Object>of("sub", "123");
    var userInfo = new GoogleOauth2UserInfo(attributes, AuthProviders.GOOGLE);

    assertThat(userInfo.getProvider()).isEqualTo(AuthProviders.GOOGLE);
  }

  @Test
  void shouldHandleMissingAttributes() {
    var attributes = Map.<String, Object>of();
    var userInfo = new GoogleOauth2UserInfo(attributes, AuthProviders.GOOGLE);

    assertThat(userInfo.getId()).isNull();
    assertThat(userInfo.getName()).isNull();
    assertThat(userInfo.getEmail()).isNull();
    assertThat(userInfo.getImageUrl()).isNull();
    assertThat(userInfo.getLocale()).isNull();
  }
}
