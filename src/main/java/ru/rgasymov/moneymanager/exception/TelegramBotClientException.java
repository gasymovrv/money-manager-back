package ru.rgasymov.moneymanager.exception;

public class TelegramBotClientException extends RuntimeException {

  public TelegramBotClientException(String msg, Throwable e) {
    super(msg, e);
  }
}
