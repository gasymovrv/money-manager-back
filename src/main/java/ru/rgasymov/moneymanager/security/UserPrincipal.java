package ru.rgasymov.moneymanager.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import ru.rgasymov.moneymanager.domain.entity.User;

public class UserPrincipal implements OAuth2User, UserDetails {

  @Getter
  @Setter
  private User businessUser;

  private final Collection<? extends GrantedAuthority> authorities;

  private Map<String, Object> attributes;

  public UserPrincipal(User user,
                       Collection<? extends GrantedAuthority> authorities) {
    this.businessUser = user;
    this.authorities = authorities;
  }

  public static UserPrincipal create(User user) {
    var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

    return new UserPrincipal(user, authorities);
  }

  public static UserPrincipal create(User user, Map<String, Object> attributes) {
    UserPrincipal userPrincipal = UserPrincipal.create(user);
    userPrincipal.setAttributes(attributes);
    return userPrincipal;
  }

  @Override
  public String getPassword() {
    return null;
  }

  @Override
  public String getUsername() {
    return businessUser.getEmail();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  @Override
  public String getName() {
    return String.valueOf(businessUser.getId());
  }
}
