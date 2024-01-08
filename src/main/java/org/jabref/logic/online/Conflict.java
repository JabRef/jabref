package org.jabref.logic.online;

import org.jabref.jabrefonline.UserChangesQuery.Node;
import org.jabref.model.entry.BibEntry;

public class Conflict {

    private BibEntry localEntry;
    private Node remoteEntry;

    public Conflict(BibEntry localEntry, Node remoteEntry) {
        this.localEntry = localEntry;
        this.remoteEntry = remoteEntry;
    }

    public BibEntry getLocalEntry() {
        return localEntry;
    }
    
    public Node getRemoteEntry() {
        return remoteEntry;
    }
}
