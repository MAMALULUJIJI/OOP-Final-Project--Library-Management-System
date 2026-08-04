package com.library;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test for the scaffold: the application context starts, which means the
 * starters resolve, the datasource connects, and Hibernate accepts the dialect.
 * Break the wiring and this fails before any feature test does.
 */
@SpringBootTest
class LibraryApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
        assertNotNull(dataSource);
    }

    @Test
    void databaseIsReachable() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.isValid(5));
        }
    }
}
