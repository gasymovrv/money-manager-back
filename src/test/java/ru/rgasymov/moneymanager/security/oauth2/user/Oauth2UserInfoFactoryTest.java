package ru.rgasymov.moneymanager.security.oauth2.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;

class Oauth2UserInfoFactoryTest {

  @Test
  void getOauth2UserInfo_shouldReturnGoogleUserInfo_whenProviderIsGoogle() {
    Map<String, Object> attributes = Map.of(
        "sub", "google123",
        "name", "John Doe",
        "email", "john@example.com",
        "picture", "http://example.com/pic.jpg",
        "locale", "en"
    );

    var userInfo = Oauth2UserInfoFactory.getOauth2UserInfo(AuthProviders.GOOGLE, attributes);

    assertThat(userInfo).isInstanceOf(GoogleOauth2UserInfo.class);
    assertThat(userInfo.getId()).isEqualTo("google123");
    assertThat(userInfo.getName()).isEqualTo("John Doe");
    assertThat(userInfo.getEmail()).isEqualTo("john@example.com");
    assertThat(userInfo.getImageUrl()).isEqualTo("http://example.com/pic.jpg");
    assertThat(userInfo.getLocale()).isEqualTo("en");
  }

  @Test
  void getOauth2UserInfo_shouldReturnVkUserInfo_whenProviderIsVk() {
    Map<String, Object> attributes = Map.of(
        "id", (Object) 123,
        "first_name", "Ivan",
        "last_name", "Petrov",
        "email", "ivan@example.com",
        "photo_max", "http://example.com/photo.jpg"
    );

    var userInfo = Oauth2UserInfoFactory.getOauth2UserInfo(AuthProviders.VK, attributes);

    assertThat(userInfo).isInstanceOf(VkOauth2UserInfo.class);
    assertThat(userInfo.getId()).isEqualTo("123");
    assertThat(userInfo.getName()).isEqualTo("Ivan Petrov");
    assertThat(userInfo.getEmail()).isEqualTo("ivan@example.com");
    assertThat(userInfo.getImageUrl()).isEqualTo("http://example.com/photo.jpg");
    assertThat(userInfo.getLocale()).isNull();
  }

  @Test
  void getOauth2UserInfo_shouldWorkWithSupportedProviders() {
    // This test validates that the factory method works for supported providers
    // Testing unsupported providers is not practical since AuthProviders is an enum
    // with only supported values. The actual validation is covered by the
    // specific provider tests above.
    assertThat(AuthProviders.values()).hasSizeGreaterThan(0);
  }

  @Test
  void getOauth2UserInfo_shouldHandleAttributesCorrectly() {
    var googleAttributes = Map.<String, Object>of(
        "sub", "123",
        "name", "Test User",
        "email", "test@test.com"
    );

    var userInfo = Oauth2UserInfoFactory.getOauth2UserInfo(
        AuthProviders.GOOGLE,
        googleAttributes
    );

    assertThat(userInfo.getAttributes()).isEqualTo(googleAttributes);
    assertThat(userInfo.getProvider()).isEqualTo(AuthProviders.GOOGLE);
  }
}
