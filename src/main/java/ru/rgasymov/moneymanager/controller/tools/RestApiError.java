package ru.rgasymov.moneymanager.controller.tools;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import ru.rgasymov.moneymanager.domain.dto.response.ErrorDto;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
final class RestApiError {

  private Integer code;

  private HttpStatus status;

  @Builder.Default
  private List<ErrorDto> errors = new ArrayList<>();
}