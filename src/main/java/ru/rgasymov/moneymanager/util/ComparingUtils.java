package ru.rgasymov.moneymanager.util;

public final class ComparingUtils {

  private ComparingUtils() {
  }

  public static <T> boolean isChanged(T oldValue, T newValue) {
    return (oldValue != null && !oldValue.equals(newValue))
        || (oldValue == null && newValue != null);
  }
}
