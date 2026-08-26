package dev.sahilbasumatary.matchservice.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;

public final class PostgresMatchHarness {

    public static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("tennisly_matches")
                    .withUsername("tennisly")
                    .withPassword("tennisly_dev")
                    .withCommand(
                            "postgres", "-c", "synchronous_commit=on", "-c", "wal_compression=on");

    private PostgresMatchHarness() {}

    public static DataSource dataSource() {
        RuntimeException localFailure = null;
        try {
            return migrate(
                    pool(
                            "jdbc:postgresql://"
                                    + env("POSTGRES_HOST", "localhost")
                                    + ":"
                                    + env("POSTGRES_PORT", "15432")
                                    + "/"
                                    + env("POSTGRES_DB_MATCHES", "tennisly_matches"),
                            env("POSTGRES_USER", "tennisly"),
                            env("POSTGRES_PASSWORD", "tennisly_dev")));
        } catch (RuntimeException ex) {
            localFailure = ex;
        }
        try {
            if (!POSTGRES.isRunning()) {
                POSTGRES.start();
            }
            return migrate(
                    pool(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        } catch (RuntimeException remote) {
            IllegalStateException failure =
                    new IllegalStateException(
                            "Postgres is required for match commit/ingest tests", localFailure);
            failure.addSuppressed(remote);
            throw failure;
        }
    }

    private static DataSource migrate(HikariDataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        return dataSource;
    }

    private static HikariDataSource pool(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(4);
        config.setInitializationFailTimeout(3_000);
        return new HikariDataSource(config);
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
