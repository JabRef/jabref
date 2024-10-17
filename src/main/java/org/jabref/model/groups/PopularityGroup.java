package org.jabref.model.groups;

import java.nio.file.Path;
import java.util.Objects;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import static org.jabref.gui.desktop.os.NativeDesktop.getOtherDataDir;

/**
 * This group contains entries that have been viewed more than once.
 * {@link StandardField#GROUPS}.
 */
public class PopularityGroup extends AbstractGroup {
    private static MVStore mvStore;

    public PopularityGroup(String name, GroupHierarchyType context) {
        super(name, context);
        mvStore = getMVStore();
    }

    public boolean contains(BibEntry entry) {
        int viewCount = getEntryViewCount(entry);
        return viewCount > 0;
    }

    @Override
    public AbstractGroup deepCopy() {
        // Create a copy of this popularity group with the same tracker and context
        return new PopularityGroup(getName(), getHierarchicalContext());
    }

    @Override
    public boolean isDynamic() {
        // This group is dynamic since it changes based on view count
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PopularityGroup other)) {
            return false;
        }
        return Objects.equals(getName(), other.getName())
                && Objects.equals(getHierarchicalContext(), other.getHierarchicalContext());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getHierarchicalContext());
    }

    /**
     * Retrieves the view count for a given BibEntry.
     *
     * @param entry The BibEntry whose view count is to be fetched.
     * @return The number of views for the BibEntry, or 0 if not found.
     */
    public static int getEntryViewCount(BibEntry entry) {
        MVMap<String, Integer> viewCounts = mvStore.openMap("entryViewCounts");
        String uniqueKey = getUniqueKeyForEntry(entry);

        // Log the entry and unique key
        System.out.println("BibEntry: " + entry);
        System.out.println("UniqueKey: " + uniqueKey);

        return viewCounts.getOrDefault(uniqueKey, 0);
    }

    public static String getUniqueKeyForEntry(BibEntry entry) {
        return entry.getField(StandardField.DOI).orElse(
                entry.getField(StandardField.KEY).orElse(
                        String.valueOf(entry.hashCode())
                )
        );
    }

    public static synchronized MVStore getMVStore() {
        if (mvStore == null) {
            Path mvStorePath = getOtherDataDir().resolve("tracking.mv");
            mvStore = new MVStore.Builder().fileName(mvStorePath.toString()).open();
        }
        return mvStore;
    }
}
