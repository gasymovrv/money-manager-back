package ru.rgasymov.moneymanager;


import org.testcontainers.containers.PostgreSQLContainer;

class PostgresContainerInitializer {

  private static final String IMAGE = "postgres:16.1";
  private static PostgreSQLContainer<?> postgreSQLContainer;

  public static PostgreSQLContainer<?> initialize() {
    if (postgreSQLContainer == null) {
      postgreSQLContainer = new PostgreSQLContainer<>(IMAGE);

      postgreSQLContainer.start();
      System.setProperty("JDBC_URL", postgreSQLContainer.getJdbcUrl());
      System.setProperty("POSTGRES_USERNAME", postgreSQLContainer.getUsername());
      System.setProperty("POSTGRES_PASSWORD", postgreSQLContainer.getPassword());
    }
    return postgreSQLContainer;
  }
}
