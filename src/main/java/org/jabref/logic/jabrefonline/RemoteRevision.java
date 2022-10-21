package org.jabref.logic.jabrefonline;

/**
 * This is essentially a [MVCC](http://en.wikipedia.org/wiki/Multiversion_concurrency_control) token value that corresponds to a version of the item saved in the server.
 */
public class RemoteRevision {
    /**
     * The unique identifier of the item.
     */
    private String nodeId;
    /**
     * A positive number that is incremented every time the item is modified.
     */
    private Integer generationId;
    /**
     * The hash of the item's content (i.e. all data except for the generationId and the hash itself).
     */
    private String hash;

    public RemoteRevision(String nodeId, Integer generationId, String hash) {
        this.nodeId = nodeId;
        this.generationId = generationId;
        this.hash = hash;
    }
    
    public String getNodeId() {
        return nodeId;
    }

    public Integer getGenerationId() {
        return generationId;
    }

    public String getHash() {
        return hash;
    }

    /**
     * Returns true if this revision is strictly newer than the given revision.
     */
    public boolean isNewerThan(RemoteRevision otherRevision) {
        return this.generationId > otherRevision.generationId;
    }
}
