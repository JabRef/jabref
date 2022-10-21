package org.jabref.logic.jabrefonline;

public class LocalRevision extends RemoteRevision {
    /**
     * Indicates whether the item is modified locally.
     */
    private boolean isDirty = false;

    public LocalRevision(String nodeId, Integer generationId, String hash) {
        super(nodeId, generationId, hash);
    }

    public boolean isDirty() {
        // TODO: Keep this state in sync with the entry's state
        return isDirty;
    }
}
