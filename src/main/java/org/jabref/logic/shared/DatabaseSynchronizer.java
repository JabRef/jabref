package org.jabref.logic.shared;

import org.jabref.model.entry.BibEntry;

public interface DatabaseSynchronizer {

    String getDBName();

    void pullChanges();

    void closeSharedDatabase();

    void registerListener(Object listener);

    void openSharedDatabase(DatabaseConnection connection) throws DatabaseNotSupportedException;

    void synchronizeSharedEntry(BibEntry bibEntry);

    void synchronizeLocalDatabase();

    DatabaseConnectionProperties getConnectionProperties();
}
