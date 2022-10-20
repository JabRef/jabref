package org.jabref.logic.jabrefonline;

import java.util.Optional;

import org.jabref.jabrefonline.UserChangesQuery;

/**
 * Provides access and handles the communication with the remote service.
 */
public interface RemoteCommunicationService {

    /**
     * Returns a list of changes that happened on the remote server since the last sync.
     */
    UserChangesQuery.Changes getChanges(String clientId, Optional<SyncCheckpoint> since);
}
