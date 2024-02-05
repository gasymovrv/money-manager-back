package ru.rgasymov.moneymanager.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.Optional;
import org.springframework.util.SerializationUtils;

public final class CookieUtils {

  private CookieUtils() {
  }

  public static Optional<Cookie> getCookie(HttpServletRequest request,
                                           String name) {
    Cookie[] cookies = request.getCookies();

    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals(name)) {
          return Optional.of(cookie);
        }
      }
    }

    return Optional.empty();
  }

  public static void addCookie(HttpServletResponse response,
                               String name,
                               String value,
                               Integer maxAge) {
    Cookie cookie = new Cookie(name, value);
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    if (maxAge != null) {
      cookie.setMaxAge(maxAge);
    }
    response.addCookie(cookie);
  }

  public static void addCookie(HttpServletResponse response,
                               String name,
                               String value) {
    addCookie(response, name, value, null);
  }

  public static void deleteCookie(HttpServletRequest request,
                                  HttpServletResponse response,
                                  String name) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals(name)) {
          cookie.setValue("");
          cookie.setPath("/");
          cookie.setMaxAge(0);
          response.addCookie(cookie);
        }
      }
    }
  }

  public static String serialize(Object object) {
    return Base64.getUrlEncoder()
        .encodeToString(SerializationUtils.serialize(object));
  }

  public static <T> T deserialize(Cookie cookie, Class<T> cls) {
    return cls.cast(SerializationUtils.deserialize(
        Base64.getUrlDecoder().decode(cookie.getValue())));
  }
}
