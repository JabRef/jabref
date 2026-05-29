package org.jabref.logic.ai;

import org.jabref.model.database.BibDatabaseContext;

public interface AiDatabaseListener extends AutoCloseable {
    void setupDatabase(BibDatabaseContext databaseContext);
}
