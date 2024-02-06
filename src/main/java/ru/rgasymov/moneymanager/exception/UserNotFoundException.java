package ru.rgasymov.moneymanager.exception;

public class UserNotFoundException extends RuntimeException {
  public UserNotFoundException(String id) {
    super(String.format("User not found by id: '%s'", id));
  }
}
