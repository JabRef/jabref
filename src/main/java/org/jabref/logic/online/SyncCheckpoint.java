package org.jabref.logic.online;

import java.time.ZonedDateTime;

import org.jabref.jabrefonline.UserChangesQuery.EndCursor;

/**
 * Checkpoints allow a sync task to be resumed from where it stopped, without having to start from the beginning.
 *
 * The checkpoint locally stored by the client signals the time of the last server change that has been integrated into the local library.
 * The checkpoint is a tuple consisting of the server time of the latest change and the highest ID of the entry in the batch; but its better to not depend on these semantics.
 */
public class SyncCheckpoint {
    
    private final ZonedDateTime timestamp;
    private final String nodeId;

    public SyncCheckpoint(ZonedDateTime timestamp, String nodeId) {
        this.timestamp = timestamp;
        this.nodeId = nodeId;
    }

    public SyncCheckpoint(EndCursor endCursor) {
        this(endCursor.lastModified, endCursor.id);
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public String getNodeId() {
        return nodeId;
    }
}
