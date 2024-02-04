package ru.rgasymov.moneymanager;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MoneyManagerApplication {

  public static void main(String[] args) {
    SpringApplication.run(MoneyManagerApplication.class, args);
  }

  @PostConstruct
  void started() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }
}
