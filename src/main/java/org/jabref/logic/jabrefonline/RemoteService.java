package org.jabref.logic.jabrefonline;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

public class RemoteService {

    private RemoteClient remoteClient;

    public RemoteService(PreferencesService preferences) {
        this.remoteClient = new RemoteClient(preferences.getJabRefOnlinePreferences());
    }

    /**
     * Performs the initial sync of the given database with the remote server.
     * 
     * It first binds the database to the remote server, then it triggers a complete sync (pull/merge/push cycle).
     */
    public void initialSync(BibDatabaseContext database) {
        if (database.getMetaData().getRemoteSettings().isPresent()) {
            throw new UnsupportedOperationException("Database is already bound to a remote server");
        }
        
        bindToAccount(database);
        sync(database);
    }

    private void bindToAccount(BibDatabaseContext database) {
        remoteClient.assertLoggedIn((client) -> {
            RemoteSettings remoteSettings = new RemoteSettings(client);
            database.getMetaData().setRemoteSettings(remoteSettings);
            return null;
        });
    }

    public void sync(BibDatabaseContext database) {
        assertBoundToAccount(database);

        // TODO: Implement sync
    }

    private void assertBoundToAccount(BibDatabaseContext database) {
        if (database.getMetaData().getRemoteSettings().isEmpty()) {
            throw new UnsupportedOperationException("Database is not bound to a remote server");
        }
    }
}
