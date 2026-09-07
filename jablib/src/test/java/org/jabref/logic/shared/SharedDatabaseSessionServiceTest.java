package org.jabref.logic.shared;

import java.util.List;

import javafx.collections.FXCollections;

import org.jabref.logic.preferences.LastFilesOpenedPreferences;
import org.jabref.logic.util.io.FileHistory;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SharedDatabaseSessionServiceTest {

    @Test
    void reconnectsDatabaseWithoutRememberedPassword() {
        DBMSConnectionProperties connectionProperties = new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL)
                .setHost("localhost")
                .setPort(5432)
                .setDatabase("jabref")
                .setUser("jabref")
                .setPassword("")
                .createDBMSConnectionProperties();
        SharedDatabaseSessionService sessionService = new SharedDatabaseSessionService(sharedDatabaseId -> connectionProperties);
        LastFilesOpenedPreferences lastFilesOpenedPreferences = new LastFilesOpenedPreferences(
                List.of(),
                null,
                FXCollections.observableArrayList("shared-database-id"),
                FileHistory.of(List.of()));

        List<SharedDatabaseSessionService.Reconnection> databasesToReconnect = sessionService.getDatabasesToReconnect(lastFilesOpenedPreferences);

        assertEquals(List.of(new SharedDatabaseSessionService.Reconnection("shared-database-id", connectionProperties)), databasesToReconnect);
    }

    @Test
    void restoresSharedDatabaseId() {
        SharedDatabaseSessionService sessionService = new SharedDatabaseSessionService();
        BibDatabaseContext databaseContext = new BibDatabaseContext();

        sessionService.restoreSharedDatabaseId(databaseContext, "shared-database-id");

        assertEquals("shared-database-id", databaseContext.getDatabase().getSharedDatabaseID().orElseThrow());
    }
}
