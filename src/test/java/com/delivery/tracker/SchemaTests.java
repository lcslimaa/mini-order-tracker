package com.delivery.tracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SchemaTests {
    @Autowired JdbcTemplate jdbc;
    @Test void migrationCreatesAllDomainTables() {
        for (String table : new String[]{"app_users", "products", "orders", "order_items"}) {
            assertThat(jdbc.queryForObject("select count(*) from " + table, Long.class)).isGreaterThanOrEqualTo(0);
        }
    }
    @Test void migrationCanReopenFileDatabase(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        var url = "jdbc:h2:file:" + directory.resolve("schema");
        var first = org.flywaydb.core.Flyway.configure().dataSource(url,"sa","").load();
        assertThat(first.migrate().migrationsExecuted).isEqualTo(1);
        var second = org.flywaydb.core.Flyway.configure().dataSource(url,"sa","").load();
        assertThat(second.migrate().migrationsExecuted).isZero();
        assertThat(second.validateWithResult().validationSuccessful).isTrue();
        try (var c = java.sql.DriverManager.getConnection(url,"sa",""); var statement = c.createStatement()) {
            assertThat(statement.executeQuery("select count(*) from orders").next()).isTrue();
        }
    }
}
