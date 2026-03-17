package org.jabref.support;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.jabref.logic.journals.JournalAbbreviationLoader;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/// Shared test utility for journal abbreviation tests that need a Postgres-backed repository.
public class JournalAbbreviationTestUtil {

    private static EmbeddedPostgres pg;
    private static DataSource dataSource;

    public static synchronized DataSource getDataSource() throws Exception {
        if (pg == null) {
            pg = EmbeddedPostgres.builder().start();
            dataSource = pg.getPostgresDatabase();
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
            }
            // Pre-populate once so parallel test workers never race on table creation
            JournalAbbreviationLoader.loadBuiltInRepository(dataSource);

            // Stop the embedded Postgres instance when the JVM shuts down
            final EmbeddedPostgres pgToClose = pg;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    pgToClose.close();
                } catch (IOException e) {
                    // best effort cleanup
                }
            }));
        }
        return dataSource;
    }
}
