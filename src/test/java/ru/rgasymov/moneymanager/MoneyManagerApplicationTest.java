package ru.rgasymov.moneymanager;

import org.junit.ClassRule;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

@ActiveProfiles("test")
@SpringBootTest
class MoneyManagerApplicationTest {

  @ClassRule
  public static final PostgreSQLContainer<?> postgreSQLContainer =
      PostgresContainerInitializer.initialize();

  @Test
  void contextLoads() {
  }
}
