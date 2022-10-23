package org.jabref.logic.jabrefonline;

import java.util.List;
import java.util.Optional;

import org.jabref.jabrefonline.UserChangesQuery;
import org.jabref.jabrefonline.UserChangesQuery.Node;

/**
 * Provides access and handles the communication with the remote service.
 */
public interface RemoteCommunicationService {

    /**
     * Returns a list of changes that happened on the remote server since the last sync.
     */
    UserChangesQuery.Changes getChanges(String clientId, Optional<SyncCheckpoint> since);

    /** 
     * Updates the given entries on the remote server and returns the accepted changes.
     */
    List<Node> updateEntries(String clientId, List<Node> entries);

    /**
     * Creates the given entries on the remote server and returns the accepted additions.
     */
    List<Node> createEntries(String clientid, List<Node> entries);
}
