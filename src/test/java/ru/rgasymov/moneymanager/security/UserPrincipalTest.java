package ru.rgasymov.moneymanager.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import ru.rgasymov.moneymanager.domain.entity.Account;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;

class UserPrincipalTest {

  @Test
  void create_shouldCreateUserPrincipalWithDefaultAuthority() {
    var user = createTestUser();

    var userPrincipal = UserPrincipal.create(user);

    assertThat(userPrincipal).isNotNull();
    assertThat(userPrincipal.getBusinessUser()).isEqualTo(user);
    assertThat(userPrincipal.getAuthorities()).hasSize(1);
    assertThat(userPrincipal.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .contains("ROLE_USER");
  }

  @Test
  void create_shouldCreateUserPrincipalWithAttributes() {
    var user = createTestUser();
    Map<String, Object> attributes = Map.of("key1", "value1", "key2", "value2");

    var userPrincipal = UserPrincipal.create(user, attributes);

    assertThat(userPrincipal).isNotNull();
    assertThat(userPrincipal.getBusinessUser()).isEqualTo(user);
    assertThat(userPrincipal.getAttributes()).isEqualTo(attributes);
    assertThat(userPrincipal.getAuthorities()).hasSize(1);
  }

  @Test
  void getUsername_shouldReturnUserEmail() {
    var user = createTestUser();
    var userPrincipal = UserPrincipal.create(user);

    var username = userPrincipal.getUsername();

    assertThat(username).isEqualTo("test@example.com");
  }

  @Test
  void getPassword_shouldReturnNull() {
    var user = createTestUser();
    var userPrincipal = UserPrincipal.create(user);

    var password = userPrincipal.getPassword();

    assertThat(password).isNull();
  }

  @Test
  void getName_shouldReturnUserId() {
    var user = createTestUser();
    var userPrincipal = UserPrincipal.create(user);

    var name = userPrincipal.getName();

    assertThat(name).isEqualTo("user123");
  }

  @Test
  void isAccountNonExpired_shouldReturnTrue() {
    var user = createTestUser();
    var userPrincipal = UserPrincipal.create(user);

    assertThat(userPrincipal.isAccountNonExpired()).isTrue();
  }

  @Test
  void isAccountNonLocked_shouldReturnTrue() {
    var user = createTestUser();
    var userPrincipal = UserPrincipal.create(user);

    assertThat(userPrincipal.isAccountNonLocked()).isTrue();
  }

  @Test
  void isCredentialsNonExpired_shouldReturnTrue() {
    var user = createTestUser();
    var userPrincipal = UserPrincipal.create(user);

    assertThat(userPrincipal.isCredentialsNonExpired()).isTrue();
  }

  @Test
  void isEnabled_shouldReturnTrue() {
    var user = createTestUser();
    var userPrincipal = UserPrincipal.create(user);

    assertThat(userPrincipal.isEnabled()).isTrue();
  }

  @Test
  void setAttributes_shouldUpdateAttributes() {
    var user = createTestUser();
    var userPrincipal = UserPrincipal.create(user);
    Map<String, Object> newAttributes = Map.of("new", "attributes");

    userPrincipal.setAttributes(newAttributes);

    assertThat(userPrincipal.getAttributes()).isEqualTo(newAttributes);
  }

  @Test
  void setBusinessUser_shouldUpdateUser() {
    var user1 = createTestUser();
    var user2 = User.builder()
        .id("user456")
        .email("new@example.com")
        .name("New User")
        .build();
    var userPrincipal = UserPrincipal.create(user1);

    userPrincipal.setBusinessUser(user2);

    assertThat(userPrincipal.getBusinessUser()).isEqualTo(user2);
  }

  private User createTestUser() {
    var account = Account.builder()
        .id(1L)
        .name("Test Account")
        .theme(AccountTheme.LIGHT)
        .currency("USD")
        .build();

    return User.builder()
        .id("user123")
        .email("test@example.com")
        .name("Test User")
        .provider(AuthProviders.GOOGLE)
        .locale("en")
        .picture("http://example.com/picture.jpg")
        .lastVisit(LocalDateTime.now())
        .currentAccount(account)
        .build();
  }
}
