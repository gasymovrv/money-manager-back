package ru.rgasymov.moneymanager.exception;

public class TelegramBotClientException extends RuntimeException {

  public TelegramBotClientException(String message) {
    super(message);
  }

  public TelegramBotClientException(Throwable cause) {
    super(cause);
  }
}
