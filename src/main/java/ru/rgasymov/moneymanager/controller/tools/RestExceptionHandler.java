package ru.rgasymov.moneymanager.controller.tools;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.rgasymov.moneymanager.exception.EmptyDataGenerationException;
import ru.rgasymov.moneymanager.util.ErrorUtils;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class RestExceptionHandler {

  /**
   * Client side errors.
   * no data found in the database
   */
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<RestApiError> handleEntityNotFoundException(EntityNotFoundException ex) {
    ErrorUtils.logException(ex, log);
    return createErrorResponse(ex, HttpStatus.NOT_FOUND);
  }

  /**
   * Client side errors.
   * validation exception, incorrect request body format
   */
  @ExceptionHandler({
      ValidationException.class,
      BindException.class,
      HttpMessageNotReadableException.class,
      MethodArgumentNotValidException.class,
      EmptyDataGenerationException.class
  })
  public ResponseEntity<RestApiError> handleValidationException(Exception ex) {
    ErrorUtils.logException(ex, log);
    return createErrorResponse(ex, HttpStatus.BAD_REQUEST);
  }

  /**
   * Remaining errors are server side.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<RestApiError> handleOtherExceptions(Exception ex) {
    ErrorUtils.logException(ex, log);
    return createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<RestApiError> createErrorResponse(Throwable err, HttpStatus status) {
    return ResponseEntity.status(status).body(
        RestApiError.builder()
            .code(status.value())
            .status(status)
            .errors(ErrorUtils.getErrorsFromStack(err))
            .build());
  }
}
