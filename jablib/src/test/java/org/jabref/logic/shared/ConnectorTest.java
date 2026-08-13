package org.jabref.logic.shared;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.testutils.category.DatabaseTest;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/// Provides connections to an embedded PostgreSQL for tests
@DatabaseTest
public class ConnectorTest implements AutoCloseable {

    private EmbeddedPostgres postgres;
    private final List<DBMSConnection> connections = new ArrayList<>();

    /// Fires up postgres on the first call; every call returns a new connection to that instance
    public DBMSConnection getTestDBMSConnection() throws SQLException, IOException, InvalidDBMSConnectionPropertiesException {
        if (postgres == null) {
            postgres = EmbeddedPostgres.builder().start();
        }
        DBMSConnectionProperties properties = new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL)
                .setHost("localhost")
                .setPort(postgres.getPort())
                .setDatabase("postgres")
                .setUser("postgres")
                .setPassword("postgres")
                .setUseSSL(false)
                .createDBMSConnectionProperties();
        DBMSConnection dbmsConnection = new DBMSConnection(properties);
        connections.add(dbmsConnection);
        return dbmsConnection;
    }

    /// Closes all connections and shuts down postgres
    @Override
    public void close() throws Exception {
        for (DBMSConnection dbmsConnection : connections) {
            dbmsConnection.getConnection().close();
        }
        if (postgres != null) {
            postgres.close();
        }
    }
}
