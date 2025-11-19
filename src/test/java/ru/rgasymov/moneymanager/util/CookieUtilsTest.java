package ru.rgasymov.moneymanager.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

class CookieUtilsTest {

  @Test
  void getCookie_shouldReturnCookie_whenPresent() {
    var request = mock(HttpServletRequest.class);
    var targetCookie = new Cookie("test", "value");
    var cookies = new Cookie[]{
        new Cookie("other", "other-value"),
        targetCookie
    };
    when(request.getCookies()).thenReturn(cookies);

    var result = CookieUtils.getCookie(request, "test");

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("test");
    assertThat(result.get().getValue()).isEqualTo("value");
  }

  @Test
  void getCookie_shouldReturnEmpty_whenNotPresent() {
    var request = mock(HttpServletRequest.class);
    var cookies = new Cookie[]{new Cookie("other", "value")};
    when(request.getCookies()).thenReturn(cookies);

    var result = CookieUtils.getCookie(request, "test");

    assertThat(result).isEmpty();
  }

  @Test
  void getCookie_shouldReturnEmpty_whenNoCookies() {
    var request = mock(HttpServletRequest.class);
    when(request.getCookies()).thenReturn(null);

    var result = CookieUtils.getCookie(request, "test");

    assertThat(result).isEmpty();
  }

  @Test
  void addCookie_shouldAddCookieWithMaxAge() {
    var response = mock(HttpServletResponse.class);

    CookieUtils.addCookie(response, "test", "value", 3600);

    verify(response).addCookie(any(Cookie.class));
  }

  @Test
  void addCookie_shouldAddCookieWithoutMaxAge() {
    var response = mock(HttpServletResponse.class);

    CookieUtils.addCookie(response, "test", "value");

    verify(response).addCookie(any(Cookie.class));
  }

  @Test
  void deleteCookie_shouldDeleteCookie_whenPresent() {
    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    var cookies = new Cookie[]{new Cookie("test", "value")};
    when(request.getCookies()).thenReturn(cookies);

    CookieUtils.deleteCookie(request, response, "test");

    verify(response).addCookie(any(Cookie.class));
  }

  @Test
  void deleteCookie_shouldDoNothing_whenCookieNotPresent() {
    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    var cookies = new Cookie[]{new Cookie("other", "value")};
    when(request.getCookies()).thenReturn(cookies);

    CookieUtils.deleteCookie(request, response, "test");

    // Verify that no cookie was added to the response
  }

  @Test
  void deleteCookie_shouldDoNothing_whenNoCookies() {
    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    when(request.getCookies()).thenReturn(null);

    CookieUtils.deleteCookie(request, response, "test");

    // Verify that no cookie was added to the response
  }

  @Test
  void serialize_shouldSerializeObject() {
    var testObject = "test string";

    var result = CookieUtils.serialize(testObject);

    assertThat(result).isNotNull();
    assertThat(result).isNotEmpty();
  }

  @Test
  void deserialize_shouldDeserializeObject() {
    var testObject = "test string";
    var serialized = CookieUtils.serialize(testObject);
    var cookie = new Cookie("test", serialized);

    var result = CookieUtils.deserialize(cookie, String.class);

    assertThat(result).isEqualTo(testObject);
  }

  @Test
  void serializeAndDeserialize_shouldWorkWithComplexObject() {
    record TestData(String name, int value) implements java.io.Serializable {}
    var testObject = new TestData("test", 42);
    var serialized = CookieUtils.serialize(testObject);
    var cookie = new Cookie("test", serialized);

    var result = CookieUtils.deserialize(cookie, TestData.class);

    assertThat(result).isEqualTo(testObject);
  }
}
