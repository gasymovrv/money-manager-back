package ru.rgasymov.moneymanager.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)
public class SwaggerConfig {

  private final BuildProperties buildProperties;

  @Bean
  public OpenAPI api() {
    return new OpenAPI()
        .info(new Info().title("Money-Manager API")
            .description("Management tool for Money-Manager application")
            .version(buildProperties.getVersion())
            .contact(new Contact().email("gasymovrv@gmail.com")))
        .externalDocs(new ExternalDocumentation()
            .description("Money-Manager on Github")
            .url("https://github.com/gasymovrv/money-manager"));
  }
}
