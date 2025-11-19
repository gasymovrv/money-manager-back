package ru.rgasymov.moneymanager.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class ErrorUtilsTest {

  @Test
  void getErrorsFromStack_shouldReturnSingleError_whenThereIsNoCause() {
    var exception = new RuntimeException("Test error");

    var errors = ErrorUtils.getErrorsFromStack(exception);

    assertThat(errors).hasSize(1);
    assertThat(errors.get(0).shortName()).isEqualTo("java.lang.RuntimeException");
    assertThat(errors.get(0).message()).isEqualTo("Test error");
  }

  @Test
  void getErrorsFromStack_shouldReturnMultipleErrors_whenThereAreCauses() {
    var rootCause = new IllegalStateException("Root cause");
    var middleCause = new IllegalArgumentException("Middle cause", rootCause);
    var topException = new RuntimeException("Top exception", middleCause);

    var errors = ErrorUtils.getErrorsFromStack(topException);

    assertThat(errors).hasSize(3);
    assertThat(errors.get(0).shortName()).isEqualTo("java.lang.RuntimeException");
    assertThat(errors.get(0).message()).isEqualTo("Top exception");
    assertThat(errors.get(1).shortName()).isEqualTo("java.lang.IllegalArgumentException");
    assertThat(errors.get(1).message()).isEqualTo("Middle cause");
    assertThat(errors.get(2).shortName()).isEqualTo("java.lang.IllegalStateException");
    assertThat(errors.get(2).message()).isEqualTo("Root cause");
  }

  @Test
  void getErrorsFromStack_shouldHandleNullMessage() {
    var exception = new RuntimeException();

    var errors = ErrorUtils.getErrorsFromStack(exception);

    assertThat(errors).hasSize(1);
    assertThat(errors.get(0).shortName()).isEqualTo("java.lang.RuntimeException");
    assertThat(errors.get(0).message()).isNull();
  }

  @Test
  void logException_shouldLogExceptionWithMessage() {
    var logger = mock(Logger.class);
    var exception = new Exception("Test message");

    ErrorUtils.logException(exception, logger);

    verify(logger).error(eq("# Test message"), eq(exception));
  }

  @Test
  void logException_shouldLogExceptionWithoutMessage() {
    var logger = mock(Logger.class);
    var exception = new Exception();

    ErrorUtils.logException(exception, logger);

    verify(logger).error(anyString(), eq(exception));
  }

  @Test
  void logException_shouldLogExceptionWithBlankMessage() {
    var logger = mock(Logger.class);
    var exception = new Exception("   ");

    ErrorUtils.logException(exception, logger);

    verify(logger).error(anyString(), eq(exception));
  }
}
