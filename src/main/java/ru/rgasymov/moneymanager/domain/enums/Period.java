package ru.rgasymov.moneymanager.domain.enums;

import static ru.rgasymov.moneymanager.constant.DateTimeFormats.COMMON_DATE_FORMAT;
import static ru.rgasymov.moneymanager.constant.DateTimeFormats.MONTH_FORMAT;
import static ru.rgasymov.moneymanager.constant.DateTimeFormats.YEAR_FORMAT;

import lombok.Getter;

@Getter
public enum Period {
  DAY(COMMON_DATE_FORMAT),
  MONTH(MONTH_FORMAT),
  YEAR(YEAR_FORMAT);

  private final String pattern;

  Period(String pattern) {
    this.pattern = pattern;
  }
}
