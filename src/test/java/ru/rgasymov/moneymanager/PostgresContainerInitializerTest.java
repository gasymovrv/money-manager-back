package ru.rgasymov.moneymanager;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test for PostgreSQL TestContainer initialization.
 */
class PostgresContainerInitializerTest {

  @Test
  void initialize_shouldStartPostgreSQLContainer() {
    var container = PostgresContainerInitializer.initialize();

    assertThat(container).isNotNull();
    assertThat(container.isRunning()).isTrue();
    assertThat(container.getJdbcUrl()).isNotNull();
    assertThat(container.getUsername()).isNotNull();
    assertThat(container.getPassword()).isNotNull();
  }

  @Test
  void initialize_shouldReturnSameInstance_whenCalledMultipleTimes() {
    var container1 = PostgresContainerInitializer.initialize();
    var container2 = PostgresContainerInitializer.initialize();

    assertThat(container1).isSameAs(container2);
  }

  @Test
  void initialize_shouldSetSystemProperties() {
    PostgresContainerInitializer.initialize();

    assertThat(System.getProperty("JDBC_URL")).isNotNull();
    assertThat(System.getProperty("POSTGRES_USERNAME")).isNotNull();
    assertThat(System.getProperty("POSTGRES_PASSWORD")).isNotNull();
  }
}
