package com.contactmanagement.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneUniqueConstraintMigrationTest {

    @Test
    void doesNotCreateConstraintOrChangeRowsWhenPhoneNumbersAreDuplicated() throws Exception {
        Path database = databasePath("phone-unique-duplicates.db");
        createContactsTable(database);
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath())) {
            connection.createStatement().execute(
                    "INSERT INTO contacts (id, email, phone) VALUES (1, 'john@example.com', '9876543210')");
            connection.createStatement().execute(
                    "INSERT INTO contacts (id, email, phone) VALUES (2, 'jane@example.com', '9876543210')");
        }

        PhoneUniqueConstraintMigration.applyIfSafe(dataSource(database));

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
             Statement statement = connection.createStatement()) {
            assertThat(uniquePhoneIndexExists(statement)).isFalse();
            try (ResultSet rows = statement.executeQuery("SELECT id, email, phone FROM contacts ORDER BY id")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong("id")).isEqualTo(1L);
                assertThat(rows.getString("email")).isEqualTo("john@example.com");
                assertThat(rows.getString("phone")).isEqualTo("9876543210");
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong("id")).isEqualTo(2L);
                assertThat(rows.getString("email")).isEqualTo("jane@example.com");
                assertThat(rows.getString("phone")).isEqualTo("9876543210");
                assertThat(rows.next()).isFalse();
            }
        }
    }

    @Test
    void createsUniquePhoneIndexWhenEveryPhoneNumberIsDifferent() throws Exception {
        Path database = databasePath("phone-unique-distinct.db");
        createContactsTable(database);
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath())) {
            connection.createStatement().execute(
                    "INSERT INTO contacts (id, email, phone) VALUES (1, 'john@example.com', '9876543210')");
            connection.createStatement().execute(
                    "INSERT INTO contacts (id, email, phone) VALUES (2, 'jane@example.com', '9123456789')");
        }

        PhoneUniqueConstraintMigration.applyIfSafe(dataSource(database));

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
             Statement statement = connection.createStatement()) {
            assertThat(uniquePhoneIndexExists(statement)).isTrue();
            statement.execute(
                    "INSERT INTO contacts (id, email, phone) VALUES (3, 'ada@example.com', '9000000001')");
        }
    }

    private static Path databasePath(String fileName) throws Exception {
        Path directory = Path.of("target");
        Files.createDirectories(directory);
        Path database = directory.resolve(fileName);
        Files.deleteIfExists(database);
        return database;
    }

    private static DriverManagerDataSource dataSource(Path database) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + database.toAbsolutePath());
        return dataSource;
    }

    private static void createContactsTable(Path database) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE contacts (
                        id integer primary key,
                        email varchar(255) not null unique,
                        phone varchar(30) not null
                    )
                    """);
        }
    }

    private static boolean uniquePhoneIndexExists(Statement statement) throws Exception {
        try (ResultSet indexes = statement.executeQuery("PRAGMA index_list('contacts')")) {
            while (indexes.next()) {
                if (indexes.getInt("unique") == 1
                        && PhoneUniqueConstraintMigration.INDEX_NAME.equals(indexes.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
