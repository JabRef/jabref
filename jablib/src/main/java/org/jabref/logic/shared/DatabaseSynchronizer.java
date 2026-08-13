package org.jabref.logic.shared;

import org.jabref.model.entry.BibEntry;

public interface DatabaseSynchronizer {

    String getDBName();

    void openSharedDatabase(DatabaseConnection connection) throws DatabaseNotSupportedException;

    void closeSharedDatabase();

    void pullChanges();

    void registerListener(Object listener);

    void synchronizeSharedEntry(BibEntry bibEntry);

    void synchronizeLocalDatabase();

    DatabaseConnectionProperties getConnectionProperties();
}
