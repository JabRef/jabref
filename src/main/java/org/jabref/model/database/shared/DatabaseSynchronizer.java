package org.jabref.model.database.shared;

import java.sql.SQLException;

import org.jabref.model.entry.BibEntry;

public interface DatabaseSynchronizer {

    String getDBName();

    void pullChanges();

    void closeSharedDatabase();

    void registerListener(Object listener);

    void openSharedDatabase(DatabaseConnection connection) throws DatabaseNotSupportedException, SQLException;

    void synchronizeSharedEntry(BibEntry bibEntry);

    void synchronizeLocalDatabase();

    DatabaseConnectionProperties getConnectionProperties();
}
