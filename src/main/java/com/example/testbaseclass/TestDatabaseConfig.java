package com.example.testbaseclass;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
// import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

@Configuration
@TestConfiguration
public class TestDatabaseConfig {

    @Value("${test.profile:test-local}")
    private String testProfile;

    private static PostgreSQLContainer<?> postgresContainer;

    @Bean
    @Primary
    public DataSource testDataSource(Environment env) {
        if ("test-container".equalsIgnoreCase(testProfile)) {
            // Start a Testcontainers Postgres if requested
            if (postgresContainer == null) {
                postgresContainer = new PostgreSQLContainer<>("postgres:15-alpine")
                        .withDatabaseName("testdb")
                        .withUsername("test")
                        .withPassword("test");
                postgresContainer.start();
            }

            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(postgresContainer.getJdbcUrl());
            ds.setUsername(postgresContainer.getUsername());
            ds.setPassword(postgresContainer.getPassword());
            ds.setDriverClassName(postgresContainer.getDriverClassName());
            return ds;
        } else {
            // default: embedded H2 DB
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .setName("testdb")
                    .build();
        }
    }
}
