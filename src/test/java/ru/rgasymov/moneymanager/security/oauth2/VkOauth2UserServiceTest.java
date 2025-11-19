package ru.rgasymov.moneymanager.security.oauth2;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

@ExtendWith(MockitoExtension.class)
class VkOauth2UserServiceTest {

  private VkOauth2UserService vkOauth2UserService;

  @BeforeEach
  void setUp() {
    vkOauth2UserService = new VkOauth2UserService();
  }

  @Test
  void loadUser_shouldThrowException_whenUserInfoUriIsMissing() {
    var clientRegistration = ClientRegistration
        .withRegistrationId("vk")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .clientId("client-id")
        .redirectUri("http://localhost/callback")
        .authorizationUri("http://localhost/oauth/authorize")
        .tokenUri("http://localhost/oauth/token")
        .userNameAttributeName("id")
        .build(); // Missing userInfoUri

    var accessToken = new OAuth2AccessToken(
        OAuth2AccessToken.TokenType.BEARER,
        "test-token",
        Instant.now(),
        Instant.now().plusSeconds(3600),
        java.util.Set.of("email")
    );

    var userRequest = new OAuth2UserRequest(
        clientRegistration,
        accessToken,
        Map.of("email", "test@test.com")
    );

    assertThatThrownBy(() -> vkOauth2UserService.loadUser(userRequest))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .hasMessageContaining("Missing required UserInfo Uri");
  }

  @Test
  void loadUser_shouldThrowException_whenUserNameAttributeIsMissing() {
    var clientRegistration = ClientRegistration
        .withRegistrationId("vk")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .clientId("client-id")
        .redirectUri("http://localhost/callback")
        .authorizationUri("http://localhost/oauth/authorize")
        .tokenUri("http://localhost/oauth/token")
        .userInfoUri("http://localhost/userinfo")
        .build(); // Missing userNameAttributeName

    var accessToken = new OAuth2AccessToken(
        OAuth2AccessToken.TokenType.BEARER,
        "test-token",
        Instant.now(),
        Instant.now().plusSeconds(3600)
    );

    var userRequest = new OAuth2UserRequest(
        clientRegistration,
        accessToken,
        Map.of("email", "test@test.com")
    );

    assertThatThrownBy(() -> vkOauth2UserService.loadUser(userRequest))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .hasMessageContaining("Missing required \"user name\" attribute");
  }
}
