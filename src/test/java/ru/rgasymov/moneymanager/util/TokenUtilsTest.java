package ru.rgasymov.moneymanager.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TokenUtilsTest {

  @Test
  void getExpiresIn_shouldReturnExpiresInValue_whenPresent() {
    Map<String, Object> params = Map.of("expires_in", 3600L);

    var result = TokenUtils.getExpiresIn(params);

    assertThat(result).isEqualTo(3600L);
  }

  @Test
  void getExpiresIn_shouldReturnZero_whenNotPresent() {
    var params = new HashMap<String, Object>();

    var result = TokenUtils.getExpiresIn(params);

    assertThat(result).isEqualTo(0L);
  }

  @Test
  void getExpiresIn_shouldHandleIntegerValue() {
    Map<String, Object> params = Map.of("expires_in", 3600);

    var result = TokenUtils.getExpiresIn(params);

    assertThat(result).isEqualTo(3600L);
  }

  @Test
  void getScopes_shouldReturnScopes_whenPresent() {
    Map<String, Object> params = Map.of("scope", "read write delete");

    var result = TokenUtils.getScopes(params);

    assertThat(result).containsExactlyInAnyOrder("read", "write", "delete");
  }

  @Test
  void getScopes_shouldReturnEmptySet_whenNotPresent() {
    var params = new HashMap<String, Object>();

    var result = TokenUtils.getScopes(params);

    assertThat(result).isEmpty();
  }

  @Test
  void getScopes_shouldHandleSingleScope() {
    Map<String, Object> params = Map.of("scope", "read");

    var result = TokenUtils.getScopes(params);

    assertThat(result).containsExactly("read");
  }

  @Test
  void getParameterValue_shouldReturnLongValue_whenPresent() {
    Map<String, Object> params = Map.of("param", 100L);

    var result = TokenUtils.getParameterValue(params, "param", 0L);

    assertThat(result).isEqualTo(100L);
  }

  @Test
  void getParameterValue_shouldReturnDefaultValue_whenNotPresent() {
    var params = new HashMap<String, Object>();

    var result = TokenUtils.getParameterValue(params, "param", 999L);

    assertThat(result).isEqualTo(999L);
  }

  @Test
  void getParameterValue_shouldConvertIntegerToLong() {
    Map<String, Object> params = Map.of("param", 100);

    var result = TokenUtils.getParameterValue(params, "param", 0L);

    assertThat(result).isEqualTo(100L);
  }

  @Test
  void getParameterValue_shouldParseStringToLong() {
    Map<String, Object> params = Map.of("param", "100");

    var result = TokenUtils.getParameterValue(params, "param", 0L);

    assertThat(result).isEqualTo(100L);
  }

  @Test
  void getParameterValue_shouldReturnDefaultValue_whenParsingFails() {
    Map<String, Object> params = Map.of("param", "invalid");

    var result = TokenUtils.getParameterValue(params, "param", 999L);

    assertThat(result).isEqualTo(999L);
  }

  @Test
  void getParameterValueString_shouldReturnValue_whenPresent() {
    Map<String, Object> params = Map.of("param", "value");

    var result = TokenUtils.getParameterValue(params, "param");

    assertThat(result).isEqualTo("value");
  }

  @Test
  void getParameterValueString_shouldReturnNull_whenNotPresent() {
    var params = new HashMap<String, Object>();

    var result = TokenUtils.getParameterValue(params, "param");

    assertThat(result).isNull();
  }

  @Test
  void getParameterValueString_shouldConvertObjectToString() {
    Map<String, Object> params = Map.of("param", 123);

    var result = TokenUtils.getParameterValue(params, "param");

    assertThat(result).isEqualTo("123");
  }
}
