package com.contactmanagement.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Component
public class PhoneUniqueConstraintMigration implements ApplicationRunner {

    static final String INDEX_NAME = "uk_contacts_phone";

    private static final Logger log = LoggerFactory.getLogger(PhoneUniqueConstraintMigration.class);

    private final DataSource dataSource;

    public PhoneUniqueConstraintMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        applyIfSafe(dataSource);
    }

    static void applyIfSafe(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            if (!tableExists(connection)) {
                return;
            }
            if (phoneUniqueIndexExists(connection)) {
                return;
            }

            List<String> duplicatePhones = findDuplicatePhones(connection);
            if (!duplicatePhones.isEmpty()) {
                log.warn(
                        "Unique phone constraint was not created. These phone numbers are already used by more than one contact: {}. Existing contacts were not changed. Remove the duplicates manually and restart the application.",
                        duplicatePhones);
                return;
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE UNIQUE INDEX " + INDEX_NAME + " ON contacts (phone)");
            }
            log.info("Created unique index {} on contacts.phone", INDEX_NAME);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not check phone numbers before creating the unique constraint", exception);
        }
    }

    private static boolean tableExists(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'contacts'")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static List<String> findDuplicatePhones(Connection connection) throws SQLException {
        List<String> phones = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT phone FROM contacts GROUP BY phone HAVING COUNT(*) > 1")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    phones.add(resultSet.getString(1));
                }
            }
        }
        return phones;
    }

    private static boolean phoneUniqueIndexExists(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet indexes = statement.executeQuery("PRAGMA index_list('contacts')")) {
            while (indexes.next()) {
                boolean unique = indexes.getInt("unique") == 1;
                String indexName = indexes.getString("name");
                if (unique && indexCoversOnlyPhone(connection, indexName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean indexCoversOnlyPhone(Connection connection, String indexName) throws SQLException {
        if (indexName == null || !indexName.matches("[A-Za-z0-9_]+")) {
            return false;
        }
        List<String> columns = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet indexInfo = statement.executeQuery("PRAGMA index_info('" + indexName + "')")) {
            while (indexInfo.next()) {
                columns.add(indexInfo.getString("name"));
            }
        }
        return columns.size() == 1 && "phone".equals(columns.get(0));
    }
}
