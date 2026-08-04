package com.library.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Smoke test for the build: proves the JDBC driver loads and schema.sql applies.
 */
class DatabaseTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void useTempDatabase() {
        Database.setUrl("jdbc:sqlite:" + tempDir.resolve("test.db"));
    }

    @AfterEach
    void restoreDefault() {
        Database.setUrl("jdbc:sqlite:library.db");
    }

    @Test
    void initializeCreatesEveryTable() throws Exception {
        Database.initialize();

        List<String> tables = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name")) {
            while (rs.next()) {
                tables.add(rs.getString("name"));
            }
        }

        assertTrue(tables.containsAll(List.of("book", "member", "loan", "reservation")), tables.toString());
    }

    @Test
    void initializeIsIdempotent() throws Exception {
        Database.initialize();
        Database.initialize();

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'book'")) {
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void foreignKeysAreEnforced() throws Exception {
        Database.initialize();

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA foreign_keys")) {
            assertEquals(1, rs.getInt(1));
        }
    }
}
