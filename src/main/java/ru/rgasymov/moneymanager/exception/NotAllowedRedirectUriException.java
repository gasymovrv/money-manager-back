package ru.rgasymov.moneymanager.exception;

public class NotAllowedRedirectUriException extends RuntimeException {
  public NotAllowedRedirectUriException(String message) {
    super(message);
  }
}
