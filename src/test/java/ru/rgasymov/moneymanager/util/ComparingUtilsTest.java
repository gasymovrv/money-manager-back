package ru.rgasymov.moneymanager.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ComparingUtilsTest {

  @Test
  void isChanged_shouldReturnFalse_whenBothValuesAreNull() {
    var result = ComparingUtils.isChanged(null, null);

    assertThat(result).isFalse();
  }

  @Test
  void isChanged_shouldReturnTrue_whenOldValueIsNullAndNewValueIsNotNull() {
    var result = ComparingUtils.isChanged(null, "new value");

    assertThat(result).isTrue();
  }

  @Test
  void isChanged_shouldReturnTrue_whenOldValueIsNotNullAndNewValueIsNull() {
    var result = ComparingUtils.isChanged("old value", null);

    assertThat(result).isTrue();
  }

  @Test
  void isChanged_shouldReturnFalse_whenValuesAreEqual() {
    var result = ComparingUtils.isChanged("same value", "same value");

    assertThat(result).isFalse();
  }

  @Test
  void isChanged_shouldReturnTrue_whenValuesAreDifferent() {
    var result = ComparingUtils.isChanged("old value", "new value");

    assertThat(result).isTrue();
  }

  @Test
  void isChanged_shouldWorkWithIntegers() {
    assertThat(ComparingUtils.isChanged(10, 10)).isFalse();
    assertThat(ComparingUtils.isChanged(10, 20)).isTrue();
    assertThat(ComparingUtils.isChanged(null, 10)).isTrue();
    assertThat(ComparingUtils.isChanged(10, null)).isTrue();
  }

  @Test
  void isChanged_shouldWorkWithCustomObjects() {
    record TestObject(String value) {}

    var obj1 = new TestObject("test");
    var obj2 = new TestObject("test");
    var obj3 = new TestObject("different");

    assertThat(ComparingUtils.isChanged(obj1, obj2)).isFalse();
    assertThat(ComparingUtils.isChanged(obj1, obj3)).isTrue();
  }
}
