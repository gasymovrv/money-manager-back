package ru.rgasymov.moneymanager.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.rgasymov.moneymanager.security.RestAuthenticationEntryPoint;
import ru.rgasymov.moneymanager.security.TokenAuthenticationFilter;
import ru.rgasymov.moneymanager.security.oauth2.CustomOauth2UserService;
import ru.rgasymov.moneymanager.security.oauth2.CustomTokenResponseConverter;
import ru.rgasymov.moneymanager.security.oauth2.HttpCookieOauth2AuthorizationRequestRepository;
import ru.rgasymov.moneymanager.security.oauth2.Oauth2AuthenticationFailureHandler;
import ru.rgasymov.moneymanager.security.oauth2.Oauth2AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(
    securedEnabled = true,
    jsr250Enabled = true
)
public class SecurityConfig {

  private static final String BASE_URL = "/";

  @Value("${server.api-base-url}")
  private String apiBaseUrl;

  @Value("${security.allowed-origins}")
  private List<String> allowedOrigins;

  private final CustomOauth2UserService customOauth2UserService;

  private final Oauth2AuthenticationSuccessHandler authenticationSuccessHandler;

  private final Oauth2AuthenticationFailureHandler authenticationFailureHandler;

  private final TokenAuthenticationFilter tokenAuthenticationFilter;

  private final HttpCookieOauth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

  //@Bean
  //public TokenAuthenticationFilter tokenAuthenticationFilter() {
  //  return new TokenAuthenticationFilter();
  //}
  //
  //@Bean
  //public HttpCookieOauth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
  //  return new HttpCookieOauth2AuthorizationRequestRepository();
  //}

  @Bean
  public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
      accessTokenResponseClient() {
    var tokenResponseHttpMessageConverter = new OAuth2AccessTokenResponseHttpMessageConverter();
    tokenResponseHttpMessageConverter
        .setAccessTokenResponseConverter(new CustomTokenResponseConverter());

    var restTemplate = new RestTemplate(
        List.of(new FormHttpMessageConverter(), tokenResponseHttpMessageConverter));
    restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

    var accessTokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();
    accessTokenResponseClient.setRestOperations(restTemplate);
    return accessTokenResponseClient;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    final var configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(List.of("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH"));
    configuration.setAllowCredentials(true);
    configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));

    final var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(it -> it.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .addFilterBefore(new ErrorFilter(apiBaseUrl), AuthorizationFilter.class)
        .exceptionHandling(
            it -> it.authenticationEntryPoint(new RestAuthenticationEntryPoint(apiBaseUrl)))
        .authorizeHttpRequests(it -> it
            .requestMatchers(
                BASE_URL,
                "/login",
                apiBaseUrl + "/version",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/favicon.ico",
                "/static/**").permitAll()
            .requestMatchers("/auth/**", "/oauth2/**").permitAll()
            .anyRequest().hasRole("USER")
        )
        .oauth2Login(oauth2LoginConfigurer -> oauth2LoginConfigurer
            .authorizationEndpoint(it -> it
                .baseUri("/oauth2/authorize")
                .authorizationRequestRepository(cookieAuthorizationRequestRepository)
            )
            .redirectionEndpoint(it -> it.baseUri("/oauth2/callback/*"))
            .userInfoEndpoint(it -> it.userService(customOauth2UserService))
            .tokenEndpoint(it -> it.accessTokenResponseClient(accessTokenResponseClient()))

            .successHandler(authenticationSuccessHandler)
            .failureHandler(authenticationFailureHandler)
        );

    // Add our custom Token based authentication filter
    http.addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
