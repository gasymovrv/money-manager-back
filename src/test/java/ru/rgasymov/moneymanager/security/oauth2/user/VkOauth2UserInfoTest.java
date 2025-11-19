package ru.rgasymov.moneymanager.security.oauth2.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;

class VkOauth2UserInfoTest {

  @Test
  void getId_shouldReturnIdAttributeAsString() {
    var attributes = Map.<String, Object>of("id", 12345);
    var userInfo = new VkOauth2UserInfo(attributes, AuthProviders.VK);

    var id = userInfo.getId();

    assertThat(id).isEqualTo("12345");
  }

  @Test
  void getName_shouldCombineFirstAndLastName() {
    var attributes = Map.<String, Object>of(
        "first_name", "Ivan",
        "last_name", "Petrov"
    );
    var userInfo = new VkOauth2UserInfo(attributes, AuthProviders.VK);

    var name = userInfo.getName();

    assertThat(name).isEqualTo("Ivan Petrov");
  }

  @Test
  void getEmail_shouldReturnEmailAttribute() {
    var attributes = Map.<String, Object>of("email", "ivan@example.com");
    var userInfo = new VkOauth2UserInfo(attributes, AuthProviders.VK);

    var email = userInfo.getEmail();

    assertThat(email).isEqualTo("ivan@example.com");
  }

  @Test
  void getImageUrl_shouldReturnPhotoMaxAttribute() {
    var attributes = Map.<String, Object>of("photo_max", "http://example.com/photo.jpg");
    var userInfo = new VkOauth2UserInfo(attributes, AuthProviders.VK);

    var imageUrl = userInfo.getImageUrl();

    assertThat(imageUrl).isEqualTo("http://example.com/photo.jpg");
  }

  @Test
  void getLocale_shouldReturnNull() {
    var attributes = Map.<String, Object>of("id", 123);
    var userInfo = new VkOauth2UserInfo(attributes, AuthProviders.VK);

    var locale = userInfo.getLocale();

    assertThat(locale).isNull();
  }

  @Test
  void getProvider_shouldReturnVk() {
    var attributes = Map.<String, Object>of("id", 123);
    var userInfo = new VkOauth2UserInfo(attributes, AuthProviders.VK);

    assertThat(userInfo.getProvider()).isEqualTo(AuthProviders.VK);
  }

  @Test
  void getName_shouldHandleNullFirstName() {
    var attributes = Map.<String, Object>of("last_name", "Petrov");
    var userInfo = new VkOauth2UserInfo(attributes, AuthProviders.VK);

    var name = userInfo.getName();

    assertThat(name).isEqualTo("null Petrov");
  }

  @Test
  void getName_shouldHandleNullLastName() {
    var attributes = Map.<String, Object>of("first_name", "Ivan");
    var userInfo = new VkOauth2UserInfo(attributes, AuthProviders.VK);

    var name = userInfo.getName();

    assertThat(name).isEqualTo("Ivan null");
  }

  @Test
  void getId_shouldHandleStringId() {
    var attributes = Map.<String, Object>of("id", "12345");
    var userInfo = new VkOauth2UserInfo(attributes, AuthProviders.VK);

    var id = userInfo.getId();

    assertThat(id).isEqualTo("12345");
  }

  @Test
  void getAttributes_shouldReturnAllAttributes() {
    var attributes = Map.<String, Object>of(
        "id", 123,
        "first_name", "Ivan",
        "last_name", "Petrov",
        "email", "ivan@example.com",
        "photo_max", "http://example.com/photo.jpg"
    );
    var userInfo = new VkOauth2UserInfo(attributes, AuthProviders.VK);

    assertThat(userInfo.getAttributes()).isEqualTo(attributes);
  }
}
