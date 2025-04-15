package org.jabref.model.entry;

import java.util.Objects;

import com.google.common.base.MoreObjects;
import org.jspecify.annotations.NonNull;

/**
 * Stores all information needed to manage entries on a shared (SQL) database.
 */
public class SharedBibEntryData implements Comparable<SharedBibEntryData> {

    // This id is set by the remote database system (DBS).
    // It has to be unique on remote DBS for all connected JabRef instances.
    // The old id above does not satisfy this requirement.
    // This is "ID" in JabDrive sync
    private String sharedIdAsString;

    private int sharedIdAsInt;

    // Needed for version controlling if used on shared database
    // This is "Revision" in JabDrive sync
    private int version;

    public SharedBibEntryData() {
        this.sharedIdAsString = "";
        this.sharedIdAsInt = -1;
        this.version = 1;
    }

    /**
     * @return Empty string if no sharedId is set yet
     */
    public String getSharedIdAsString() {
        return sharedIdAsString;
    }

    /**
     * @return -1 if no sharedId is set yet
     */
    public int getSharedIdAsInt() {
        return sharedIdAsInt;
    }

    public void setSharedId(@NonNull String sharedIdAsString) {
        this.sharedIdAsString = sharedIdAsString;
        try {
            this.sharedIdAsInt = Integer.parseInt(sharedIdAsString);
        } catch (NumberFormatException e) {
            this.sharedIdAsInt = Objects.hash(sharedIdAsString);
        }
    }

    public void setSharedId(@NonNull int sharedId) {
        this.sharedIdAsString = String.valueOf(sharedId);
        this.sharedIdAsInt = sharedId;
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
                          .add("sharedId", sharedIdAsString)
                          .add("version", version)
                          .toString();
    }

    @Override
    public int compareTo(SharedBibEntryData o) {
        if (this.sharedIdAsString == o.sharedIdAsString) {
            return Integer.compare(this.version, o.version);
        } else {
            return this.sharedIdAsString.compareTo(o.sharedIdAsString);
        }
    }
}
