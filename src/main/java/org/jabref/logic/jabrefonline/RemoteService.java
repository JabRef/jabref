package org.jabref.logic.jabrefonline;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.jabrefonline.UserChangesQuery;
import org.jabref.jabrefonline.UserChangesQuery.Node;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

import com.google.common.collect.Streams;

public class RemoteService {

    private RemoteClient remoteClient;
    private RemoteCommunicationService communicationService;
    private JabRefOnlineTransformer transformer;

    public RemoteService(PreferencesService preferences) {
        this.remoteClient = new RemoteClient(preferences.getJabRefOnlinePreferences());
        this.communicationService = new JabRefOnlineService();
        this.transformer = new JabRefOnlineTransformer();
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
     * Pushes the local changes (since the last sync) to the remote server.
     * In particular, the following information is pushed:
     * - the list of entries that are marked dirty.
     * - the list of entries that are new.
     * - the list of entries that have been deleted.
     * 
     * TODO: Trigger this by saving the database
     */
    public void push(BibDatabaseContext database) {
        assertBoundToAccount(database);

        boolean needsAnotherSync = false;
        remoteClient.assertLoggedIn((client) -> {
            List<BibEntry> dirtyEntries = database.getDatabase().getEntries().stream()
                                                  .filter(entry -> entry.getRevision().map(LocalRevision::isDirty).orElse(false))
                                                  .collect(Collectors.toList());
            if (!dirtyEntries.isEmpty()) {
                var result = communicationService.updateEntries(client, dirtyEntries.stream()
                                                                                             .map(entry -> transformer.toDocument(entry))
                                                                                             .collect(Collectors.toList()));
                Streams.forEachPair(dirtyEntries.stream(), result.stream(), (entry, update) -> {
                    var revision = entry.getRevision().get();
                    if (update.wasAccepted) {
                        revision.setDirty(false);
                        revision.setGeneration(update.newGeneration);
                        revision.setHash(update.newHash);
                    } else {
                        needsAnotherSync = true;
                    }
                });
            }

            List<BibEntry> newEntries = database.getDatabase().getEntries().stream()
                                            .filter(entry -> entry.getRevision().isEmpty())
                                            .collect(Collectors.toList());
            if (!newEntries.isEmpty()) {
                var acceptedAdditions = communicationService.createEntries(client, newEntries.stream()
                                                                                             .map(entry -> transformer.toDocument(entry))
                                                                                             .collect(Collectors.toList()));
                Streams.forEachPair(newEntries.stream(), acceptedAdditions.stream(), (entry, addition) -> {
                    if (addition.wasAccepted) {
                        entry.setRevision(new LocalRevision(addition.id, addition.generation, addition.hash));
                    } else {
                        needsAnotherSync = true;
                    }
                });
            }

            // TODO: Delete entries
        });
    }

    /**
     * Merges the remote changes into the given database and return the conflicts.
     */
    private List<Conflict> mergeChanges(BibDatabaseContext database, UserChangesQuery.Changes changes) {
        List<Conflict> conflicts = new ArrayList<>();
        for (var change : changes.edges) {
            var remoteEntry = change.node;
            var localEntry = database.getDatabase().getEntryByCitationKey(remoteEntry.id);

            if (localEntry.isPresent()) {
                var remoteRevision = new RemoteRevision(remoteEntry.id, change.revision.generation, change.revision.hash);
                var localRevision = localEntry.get().getRevision().get();
                if (remoteRevision.isNewerThan(localRevision)) {
                    // The server's `Revision` is higher than the client's `Revision`
                    // If the client's entry is dirty, then the user is shown a message to resolve the conflict
                    // otherwise the client's entry is replaced by the server's one (including the revision)
                    if (localRevision.isDirty()) {
                        conflicts.add(new Conflict(localEntry.get(), remoteEntry));
                    } else {
                        database.getDatabase().removeEntry(localEntry.get());
                        database.getDatabase().insertEntry(transformer.toBibEntry(remoteEntry));
                    }
                } else if (localRevision.isNewerThan(remoteRevision)) {
                    // The server's `Revision` is lower than the client's `Revision`
                    // This should never be the case, as revisions are only increased on the server.
                    throw new IllegalStateException("Revision of local entry is higher than the remote revision");
                } else {
                    // The server's `Revision` is equal to the client's `Revision`
                    // Both entries are up-to-date and nothing has to be done.
                    // This case may happen if the library is synchronized by other means.
                }
            } else {
                database.getDatabase().insertEntry(transformer.toBibEntry(remoteEntry));
            }
        }
        // TODO: Handle tombstones
        return conflicts;
    }

    private void assertBoundToAccount(BibDatabaseContext database) {
        if (database.getMetaData().getRemoteSettings().isEmpty()) {
            throw new UnsupportedOperationException("Database is not bound to a remote server");
        }
    }
}
