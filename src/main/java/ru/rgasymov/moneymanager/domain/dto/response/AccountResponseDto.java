package ru.rgasymov.moneymanager.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;

@Schema
@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountResponseDto {

  private Long id;

  private String name;

  private AccountTheme theme;

  private String currency;

  @Builder.Default
  @JsonProperty("isDraft")
  private boolean isDraft = true;
}
