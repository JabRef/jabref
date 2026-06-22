package org.jabref.support;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.jabref.logic.journals.JournalAbbreviationLoader;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/// Shared test utility for journal abbreviation tests that need a Postgres-backed repository.
public class JournalAbbreviationTestUtil {

    private static EmbeddedPostgres embeddedPostgres;
    private static Connection connection;

    public static synchronized Connection getConnection() throws Exception {
        if (connection == null) {
            embeddedPostgres = EmbeddedPostgres.builder().start();
            DataSource dataSource = embeddedPostgres.getPostgresDatabase();
            connection = dataSource.getConnection();
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
            }
            // Pre-populate once so parallel test workers never race on table creation
            JournalAbbreviationLoader.loadBuiltInRepository(connection);

            // Stop the embedded Postgres instance when the JVM shuts down
            final EmbeddedPostgres postgresToClose = embeddedPostgres;
            final Connection connectionToClose = connection;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    connectionToClose.close();
                    postgresToClose.close();
                } catch (IOException | SQLException e) {
                    // best effort cleanup
                }
            }));
        }
        return connection;
    }
}
