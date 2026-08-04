package com.library.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Owns the single SQLite database file and hands out connections to the DAO
 * layer. Nothing above the DAO layer should reference this class.
 */
public final class Database {

    private static final String DEFAULT_URL = "jdbc:sqlite:library.db";
    private static final String SCHEMA_RESOURCE = "/schema.sql";

    private static String url = DEFAULT_URL;

    private Database() {
    }

    /**
     * Points the application at a different database, e.g. {@code jdbc:sqlite::memory:}
     * or a per-test file. Call before {@link #initialize()}.
     */
    public static void setUrl(String jdbcUrl) {
        url = jdbcUrl;
    }

    /**
     * Opens a connection with foreign keys enforced. Callers own the connection
     * and should use try-with-resources.
     */
    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return connection;
    }

    /**
     * Creates the database file on first run and applies {@code schema.sql}.
     * Safe to call on every startup — every statement in the schema is idempotent.
     */
    public static void initialize() throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : splitStatements(readSchema())) {
                statement.execute(sql);
            }
        }
    }

    /**
     * Splits schema.sql into individual statements. Line comments are dropped
     * first so that a {@code ;} inside a comment does not split a statement in
     * two. Good enough for a schema file we control; it would not survive a
     * {@code --} inside a string literal.
     */
    private static List<String> splitStatements(String schema) {
        String withoutComments = schema.lines()
                .filter(line -> !line.stripLeading().startsWith("--"))
                .collect(Collectors.joining("\n"));

        List<String> statements = new ArrayList<>();
        for (String sql : withoutComments.split(";")) {
            if (!sql.isBlank()) {
                statements.add(sql);
            }
        }
        return statements;
    }

    private static String readSchema() {
        try (InputStream in = Database.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource " + SCHEMA_RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + SCHEMA_RESOURCE, e);
        }
    }
}
