package org.jabref.logic.shared;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.jabref.testutils.category.DatabaseTest;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Stores the credentials for the test systems
 */
@DatabaseTest
public class ConnectorTest implements AutoCloseable {

    private EmbeddedPostgres postgres;
    private Connection connection;

    /**
     * Fires up a new postgres
     */
    public DBMSConnection getTestDBMSConnection() throws SQLException, IOException {
        postgres = EmbeddedPostgres.builder().start();
        String url = postgres.getJdbcUrl("postgres", "postgres");
        connection = DriverManager.getConnection(url, "postgres", "postgres");
        return new DBMSConnection(connection);
    }

    /**
     * Closes the connection and shuts down postgres
     */
    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
        if (postgres != null) {
            postgres.close();
        }
    }
}
