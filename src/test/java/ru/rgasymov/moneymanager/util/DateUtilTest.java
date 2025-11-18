package ru.rgasymov.moneymanager.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DateUtilTest {

  @Test
  void getDatesBetweenInclusive_shouldReturnAllDatesInRange() {
    var startDate = LocalDate.of(2024, 1, 1);
    var endDate = LocalDate.of(2024, 1, 5);

    var result = DateUtil.getDatesBetweenInclusive(startDate, endDate);

    assertThat(result).hasSize(5);
    assertThat(result).containsExactly(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 1, 2),
        LocalDate.of(2024, 1, 3),
        LocalDate.of(2024, 1, 4),
        LocalDate.of(2024, 1, 5)
    );
  }

  @Test
  void getDatesBetweenInclusive_shouldReturnSingleDate_whenStartAndEndAreSame() {
    var date = LocalDate.of(2024, 1, 15);

    var result = DateUtil.getDatesBetweenInclusive(date, date);

    assertThat(result).hasSize(1);
    assertThat(result).containsExactly(date);
  }

  @Test
  void getDatesBetweenInclusive_shouldThrowException_whenStartIsAfterEnd() {
    var startDate = LocalDate.of(2024, 1, 10);
    var endDate = LocalDate.of(2024, 1, 5);

    org.assertj.core.api.Assertions.assertThatThrownBy(() ->
        DateUtil.getDatesBetweenInclusive(startDate, endDate))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void getFirstDateOfMonth_shouldReturnFirstDayOfMonth() {
    var date = LocalDate.of(2024, 3, 15);

    var result = DateUtil.getFirstDateOfMonth(date);

    assertThat(result).isEqualTo(LocalDate.of(2024, 3, 1));
  }

  @Test
  void getFirstDateOfMonth_shouldHandleFebruaryInLeapYear() {
    var date = LocalDate.of(2024, 2, 15);

    var result = DateUtil.getFirstDateOfMonth(date);

    assertThat(result).isEqualTo(LocalDate.of(2024, 2, 1));
  }

  @Test
  void getLastDateOfMonth_shouldReturnLastDayOfMonth() {
    var date = LocalDate.of(2024, 3, 15);

    var result = DateUtil.getLastDateOfMonth(date);

    assertThat(result).isEqualTo(LocalDate.of(2024, 3, 31));
  }

  @Test
  void getLastDateOfMonth_shouldReturnCorrectDayForFebruaryInLeapYear() {
    var date = LocalDate.of(2024, 2, 15);

    var result = DateUtil.getLastDateOfMonth(date);

    assertThat(result).isEqualTo(LocalDate.of(2024, 2, 29));
  }

  @Test
  void getLastDateOfMonth_shouldReturnCorrectDayForFebruaryInNonLeapYear() {
    var date = LocalDate.of(2023, 2, 15);

    var result = DateUtil.getLastDateOfMonth(date);

    assertThat(result).isEqualTo(LocalDate.of(2023, 2, 28));
  }

  @Test
  void getLastDateOfMonth_shouldReturnCorrectDayForMonthWith30Days() {
    var date = LocalDate.of(2024, 4, 15);

    var result = DateUtil.getLastDateOfMonth(date);

    assertThat(result).isEqualTo(LocalDate.of(2024, 4, 30));
  }
}
