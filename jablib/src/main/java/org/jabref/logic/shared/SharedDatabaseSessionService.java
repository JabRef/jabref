package org.jabref.logic.shared;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jabref.logic.preferences.LastFilesOpenedPreferences;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.jspecify.annotations.NullMarked;

/// Manages the persisted state of shared databases between application sessions.
@NullMarked
public class SharedDatabaseSessionService {

    private final Function<String, DBMSConnectionProperties> connectionPropertiesFactory;

    public SharedDatabaseSessionService() {
        this(sharedDatabaseId -> new DBMSConnectionProperties(new SharedDatabasePreferences(sharedDatabaseId)));
    }

    SharedDatabaseSessionService(Function<String, DBMSConnectionProperties> connectionPropertiesFactory) {
        this.connectionPropertiesFactory = connectionPropertiesFactory;
    }

    /// Returns all shared databases remembered for reconnection, including databases with an empty password.
    public List<Reconnection> getDatabasesToReconnect(LastFilesOpenedPreferences lastFilesOpenedPreferences) {
        return List.copyOf(lastFilesOpenedPreferences.getLastSharedDatabasesOpened()).stream()
                   .map(sharedDatabaseId -> new Reconnection(sharedDatabaseId, connectionPropertiesFactory.apply(sharedDatabaseId)))
                   .toList();
    }

    /// Persists the connection settings of the shared databases that remain open until quit is confirmed.
    public void persistConnections(Map<String, DatabaseConnectionProperties> sharedDatabases) {
        sharedDatabases.forEach((sharedDatabaseId, connectionProperties) ->
                new SharedDatabasePreferences(sharedDatabaseId).putAllDBMSConnectionProperties(connectionProperties));
    }

    /// Restores the identifier used to find a shared database's stored connection settings.
    public void restoreSharedDatabaseId(BibDatabaseContext databaseContext, String sharedDatabaseId) {
        databaseContext.getDatabase().setSharedDatabaseID(sharedDatabaseId);
    }

    public record Reconnection(String sharedDatabaseId, DBMSConnectionProperties connectionProperties) {
    }
}
