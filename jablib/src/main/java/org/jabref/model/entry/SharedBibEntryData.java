package org.jabref.model.entry;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/// Stores all information needed to manage entries on a shared (SQL) database.
public class SharedBibEntryData implements Comparable<SharedBibEntryData> {

    // This id is set by the remote database system (DBS).
    // It has to be unique on remote DBS for all connected JabRef instances.
    // The old id above does not satisfy this requirement.
    // This is "ID" in JabDrive sync
    private int sharedID;

    // Needed for version controlling if used on shared database
    // This is "Revision" in JabDrive sync
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("sharedID", sharedID)
                          .add("version", version)
                          .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SharedBibEntryData other)) {
            return false;
        }
        return sharedID == other.sharedID && version == other.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sharedID, version);
    }

    @Override
    public int compareTo(SharedBibEntryData o) {
        if (this.sharedID == o.sharedID) {
            return Integer.compare(this.version, o.version);
        } else {
            return Integer.compare(this.sharedID, o.sharedID);
        }
    }
}
