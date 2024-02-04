package ru.rgasymov.moneymanager.domain.enums;

import lombok.Getter;

@Getter
public enum SavingFieldToSort {
  DATE("date"),
  SAVING_VALUE("value");

  private final String fieldName;

  SavingFieldToSort(String fieldName) {
    this.fieldName = fieldName;
  }
}
