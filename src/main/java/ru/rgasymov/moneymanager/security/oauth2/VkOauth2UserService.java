package ru.rgasymov.moneymanager.security.oauth2;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequestEntityConverter;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class VkOauth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private static final String MISSING_USER_INFO_URI_ERROR_CODE = "missing_user_info_uri";
  private static final String MISSING_USER_NAME_ATTRIBUTE_ERROR_CODE =
      "missing_user_name_attribute";
  private static final String INVALID_USER_INFO_RESPONSE_ERROR_CODE = "invalid_user_info_response";
  private static final String EMAIL_KEY = "email";

  private static final Converter<OAuth2UserRequest, RequestEntity<?>> requestEntityConverter =
      new OAuth2UserRequestEntityConverter();

  private static final ParameterizedTypeReference<Map<String, Object>> PARAMETERIZED_RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {
      };

  private final RestOperations restOperations;

  public VkOauth2UserService() {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
    this.restOperations = restTemplate;
  }

  @Override
  public OAuth2User loadUser(
      OAuth2UserRequest oauth2UserRequest) throws OAuth2AuthenticationException {

    checkOauth2UserRequest(oauth2UserRequest);

    var request = requestEntityConverter.convert(oauth2UserRequest);
    var response = getResponse(oauth2UserRequest, request);

    //extract attributes from the "response" wrapper
    Map<String, Object> body = response.getBody();
    var valueList = (ArrayList) body.get("response");
    var userAttributes = (Map<String, Object>) valueList.get(0);
    userAttributes.put(EMAIL_KEY, oauth2UserRequest.getAdditionalParameters().get(EMAIL_KEY));
    Set<GrantedAuthority> authorities = new LinkedHashSet<>();
    authorities.add(new OAuth2UserAuthority(userAttributes));
    OAuth2AccessToken token = oauth2UserRequest.getAccessToken();
    for (String authority : token.getScopes()) {
      authorities.add(new SimpleGrantedAuthority("SCOPE_" + authority));
    }

    var userNameAttributeName = oauth2UserRequest
        .getClientRegistration()
        .getProviderDetails()
        .getUserInfoEndpoint()
        .getUserNameAttributeName();
    return new DefaultOAuth2User(authorities, userAttributes, userNameAttributeName);
  }

  private void checkOauth2UserRequest(OAuth2UserRequest oauth2UserRequest) {
    if (!StringUtils.hasText(
        oauth2UserRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint()
            .getUri())) {
      OAuth2Error oauth2Error = new OAuth2Error(
          MISSING_USER_INFO_URI_ERROR_CODE,
          "Missing required UserInfo Uri in UserInfoEndpoint for Client Registration: "
              + oauth2UserRequest.getClientRegistration().getRegistrationId(),
          null
      );
      throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
    }

    var userNameAttributeName = oauth2UserRequest.getClientRegistration().getProviderDetails()
        .getUserInfoEndpoint().getUserNameAttributeName();
    if (!StringUtils.hasText(userNameAttributeName)) {
      OAuth2Error oauth2Error = new OAuth2Error(
          MISSING_USER_NAME_ATTRIBUTE_ERROR_CODE,
          "Missing required \"user name\" attribute name in "
              + "UserInfoEndpoint for Client Registration: "
              + oauth2UserRequest.getClientRegistration().getRegistrationId(),
          null
      );
      throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
    }
  }

  private ResponseEntity<Map<String, Object>> getResponse(OAuth2UserRequest userRequest,
                                                          RequestEntity<?> request) {
    try {
      return this.restOperations.exchange(request, PARAMETERIZED_RESPONSE_TYPE);
    } catch (OAuth2AuthorizationException ex) {
      OAuth2Error oauth2Error = ex.getError();
      var errorDetails = new StringBuilder();
      errorDetails.append("Error details: [");
      errorDetails.append("UserInfo Uri: ").append(
          userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri());
      errorDetails.append(", Error Code: ").append(oauth2Error.getErrorCode());
      if (oauth2Error.getDescription() != null) {
        errorDetails.append(", Error Description: ").append(oauth2Error.getDescription());
      }
      errorDetails.append("]");
      oauth2Error = new OAuth2Error(INVALID_USER_INFO_RESPONSE_ERROR_CODE,
          "An error occurred while attempting to retrieve the UserInfo Resource: "
              + errorDetails, null);
      throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), ex);
    } catch (RestClientException ex) {
      OAuth2Error oauth2Error = new OAuth2Error(INVALID_USER_INFO_RESPONSE_ERROR_CODE,
          "An error occurred while attempting to retrieve the UserInfo Resource: "
              + ex.getMessage(), null);
      throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), ex);
    }
  }
}
