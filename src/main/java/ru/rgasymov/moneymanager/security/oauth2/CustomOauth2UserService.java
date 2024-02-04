package ru.rgasymov.moneymanager.security.oauth2;

import java.time.LocalDateTime;
import java.util.Currency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import ru.rgasymov.moneymanager.domain.entity.Account;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;
import ru.rgasymov.moneymanager.exception.Oauth2AuthenticationProcessingException;
import ru.rgasymov.moneymanager.security.UserPrincipal;
import ru.rgasymov.moneymanager.security.oauth2.user.Oauth2UserInfo;
import ru.rgasymov.moneymanager.security.oauth2.user.Oauth2UserInfoFactory;
import ru.rgasymov.moneymanager.service.UserService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOauth2UserService extends DefaultOAuth2UserService {

  private final UserService userService;
  private final VkOauth2UserService vkOauth2UserService;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest request)
      throws OAuth2AuthenticationException {
    OAuth2User oauth2User;
    if (request.getClientRegistration().getRegistrationId().equals(AuthProviders.VK.getId())) {
      oauth2User = vkOauth2UserService.loadUser(request);
    } else {
      oauth2User = super.loadUser(request);
    }

    try {
      return processOauth2User(request, oauth2User);
    } catch (AuthenticationException ex) {
      throw ex;
    } catch (Exception ex) {
      // Throwing an instance of AuthenticationException
      // will trigger the OAuth2AuthenticationFailureHandler
      throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
    }
  }

  private OAuth2User processOauth2User(OAuth2UserRequest request, OAuth2User authUser) {
    var provider = AuthProviders.of(request.getClientRegistration().getRegistrationId());

    var userInfo = Oauth2UserInfoFactory.getOauth2UserInfo(
        provider,
        authUser.getAttributes());
    if (StringUtils.isEmpty(userInfo.getId())) {
      throw new Oauth2AuthenticationProcessingException("User id not found from OAuth2 provider");
    }

    var user = userService.findById(userInfo.getId())
        .map(existingUser -> updateExistingUser(existingUser, userInfo))
        .orElseGet(() -> registerNewUser(userInfo));

    log.info("# User was successfully logged in: {}", user);
    return UserPrincipal.create(user, authUser.getAttributes());
  }

  private User registerNewUser(Oauth2UserInfo userInfo) {
    var newUser = new User();
    newUser.setId(userInfo.getId());
    newUser.setName(userInfo.getName());
    newUser.setEmail(userInfo.getEmail());
    newUser.setPicture(userInfo.getImageUrl());
    newUser.setLocale(userInfo.getLocale());
    newUser.setProvider(userInfo.getProvider());
    newUser.setLastVisit(LocalDateTime.now());

    var account = Account.builder()
        .user(newUser)
        .name("Default account")
        .theme(AccountTheme.LIGHT)
        .currency(Currency.getInstance("USD").getCurrencyCode())
        .build();
    newUser.setCurrentAccount(account);
    return userService.save(newUser);
  }

  private User updateExistingUser(User existingUser, Oauth2UserInfo userInfo) {
    existingUser.setName(userInfo.getName());
    existingUser.setPicture(userInfo.getImageUrl());
    existingUser.setLocale(userInfo.getLocale());
    existingUser.setProvider(userInfo.getProvider());
    existingUser.setLastVisit(LocalDateTime.now());
    return userService.save(existingUser);
  }

}
