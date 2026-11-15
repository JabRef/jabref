package org.jabref.logic.shared;

import java.sql.SQLException;

import org.jabref.model.entry.BibEntry;

public interface DatabaseSynchronizer {

    String getDBName();

    void setDBName(String dbName);

    void openSharedDatabase(DatabaseConnection connection) throws DatabaseNotSupportedException, SQLException;

    void closeSharedDatabase();

    /// Applies pending model changes on the model thread before asynchronous shutdown.
    void prepareForClose();

    /// Flushes pending changes and closes the connection after [#prepareForClose].
    void finishClose();

    void pullChanges();

    void registerListener(Object listener);

    void synchronizeSharedEntry(BibEntry bibEntry);

    void synchronizeLocalDatabase();

    DatabaseConnectionProperties getConnectionProperties();
}
