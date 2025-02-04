package ru.rgasymov.moneymanager.util;

import java.time.LocalDate;
import java.util.List;

public final class DateUtil {

  private DateUtil() {
  }

  public static List<LocalDate> getDatesBetweenInclusive(LocalDate startDate, LocalDate endDate) {
    return startDate.datesUntil(endDate.plusDays(1)).toList();
  }

  public static LocalDate getFirstDateOfMonth(LocalDate date) {
    return LocalDate.of(date.getYear(), date.getMonth(), 1);
  }

  public static LocalDate getLastDateOfMonth(LocalDate date) {
    return LocalDate.of(date.getYear(), date.getMonth(), date.lengthOfMonth());
  }
}
