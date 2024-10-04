package org.jabref.model.entry;

import com.google.common.base.MoreObjects;

/**
 * Stores all information needed to manage entries on a shared (SQL) database.
 */
public class SharedBibEntryData implements Comparable<SharedBibEntryData> {

    // This id is set by the remote database system (DBS).
    // It has to be unique on remote DBS for all connected JabRef instances.
    // The old id above does not satisfy this requirement.
    // This is "ID" in JabDrive sync
    private int sharedId;

    // Needed for version controlling if used on shared database
    // This is "Revision" in JabDrive sync
    private int version;

    public SharedBibEntryData() {
        this.sharedId = -1;
        this.version = 1;
    }

    public int getSharedId() {
        return sharedId;
    }

    public void setSharedId(int sharedId) {
        this.sharedId = sharedId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("sharedId", sharedId)
                          .add("version", version)
                          .toString();
    }

    @Override
    public int compareTo(SharedBibEntryData o) {
        if (this.sharedId == o.sharedId) {
            return Integer.compare(this.version, o.version);
        } else {
            return Integer.compare(this.sharedId, o.sharedId);
        }
    }
}
