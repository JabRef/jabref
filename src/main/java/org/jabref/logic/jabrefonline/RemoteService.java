package org.jabref.logic.jabrefonline;

import java.util.List;
import java.util.Optional;

import org.jabref.jabrefonline.UserChangesQuery;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

public class RemoteService {

    private RemoteClient remoteClient;
    private RemoteCommunicationService communicationService;

    public RemoteService(PreferencesService preferences) {
        this.remoteClient = new RemoteClient(preferences.getJabRefOnlinePreferences());
        this.communicationService = new JabRefOnlineService();
    }

    /**
     * Performs the initial sync of the given database with the remote server.
     * 
     * It first binds the local database to the remote account, then it triggers a complete sync (pull/merge/push cycle).
     */
    public void initialSync(BibDatabaseContext database) {
        if (database.getMetaData().getRemoteSettings().isPresent()) {
            throw new UnsupportedOperationException("Database is already bound to a remote server");
        }
        
        bindToAccount(database);
        sync(database);
    }

    /**
     * Binds the given local database to the remote account.
     */
    private void bindToAccount(BibDatabaseContext database) {
        remoteClient.assertLoggedIn((client) -> {
            RemoteSettings remoteSettings = new RemoteSettings(client);
            database.getMetaData().setRemoteSettings(remoteSettings);
            return null;
        });
    }

    /**
     * Performs a sync of the given database with the remote server.
     * 
     * It starts a pull/merge/push cycle, by first pulling the remote changes.
     * If there are no conflicts, the local changes are pushed to the remote server.
     * If there are conflicts, this method stops after pulling the remote changes and gives the user the chance to resolve the conflicts.
     */
    public void sync(BibDatabaseContext database) {
        assertBoundToAccount(database);

        // TODO: Implement sync
        pull(database);
    }

    /**
     * Pulls the remote changes (since the last sync) into the given database.
     */
    public void pull(BibDatabaseContext database) {
        Optional<SyncCheckpoint> lastSync = database.getMetaData().getRemoteSettings().get().getLastSync();
        pull(database, lastSync);
    }

    /**
     * Pulls the remote changes since the given checkpoint into the given database.
     */
    public void pull(BibDatabaseContext database, Optional<SyncCheckpoint> since) {
        assertBoundToAccount(database);
        
        var changes = remoteClient.assertLoggedIn((client) -> {
            return communicationService.getChanges(client, since);
        });
        var conflicts = mergeChanges(database, changes);
        if (!conflicts.isEmpty()) {
            // TODO implement conflict to gui
        }
        if (changes.pageInfo.hasNextPage) {
            pull(database, Optional.of(new SyncCheckpoint(changes.pageInfo.endCursor)));
        }
    }

    /**
     * Merges the remote changes into the given database and return the conflicts.
     */
    private List<Conflict> mergeChanges(BibDatabaseContext database, UserChangesQuery.Changes changes) {
        // TODO: Implement merge
    }

    private void assertBoundToAccount(BibDatabaseContext database) {
        if (database.getMetaData().getRemoteSettings().isEmpty()) {
            throw new UnsupportedOperationException("Database is not bound to a remote server");
        }
    }
}
