package org.jabref.model.entry;

/**
 * Stores all information needed to manage entries on a shared (SQL) database.
 */
public class SharedBibEntryData {

    // This id is set by the remote database system (DBS).
    // It has to be unique on remote DBS for all connected JabRef instances.
    // The old id above does not satisfy this requirement.
    private int sharedID;

    // Needed for version controlling if used on shared database
    private int version;

    public SharedBibEntryData() {
        this.sharedID = -1;
        this.version = 1;
    }

    public int getSharedID() {
        return sharedID;
    }

    public void setSharedID(int sharedID) {
        this.sharedID = sharedID;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
