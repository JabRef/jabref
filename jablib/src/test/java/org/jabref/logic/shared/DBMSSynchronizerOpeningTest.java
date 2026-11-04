package org.jabref.logic.shared;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class DBMSSynchronizerOpeningTest {

    @TempDir
    Path offlineChangesDirectory;

    @Test
    void failedOpeningReleasesTheConnection() throws SQLException {
        AtomicBoolean closed = new AtomicBoolean();
        // Mockito cannot mock java.sql types on the module path
        Connection connection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class}, (_, method, _) -> switch (method.getName()) {
            case "prepareStatement" ->
                    throw new SQLException("permission denied");
            case "close" -> {
                closed.set(true);
                yield null;
            }
            default ->
                    null;
        });
        DatabaseConnection databaseConnection = mock(DatabaseConnection.class);
        when(databaseConnection.getConnection()).thenReturn(connection);
        when(databaseConnection.getProperties()).thenReturn(mock(DBMSConnectionProperties.class));

        DBMSSynchronizer synchronizer = new DBMSSynchronizer(
                new BibDatabaseContext(),
                ',',
                mock(FieldPreferences.class),
                GlobalCitationKeyPatterns.fromPattern("[auth][year]"),
                new DummyFileUpdateMonitor(),
                "UserAndHost",
                new CurrentThreadTaskExecutor(),
                Runnable::run,
                Runnable::run,
                offlineChangesDirectory);

        assertThrows(SQLException.class, () -> synchronizer.openSharedDatabase(databaseConnection));
        assertTrue(closed.get());
    }
}
