package org.jabref.model.groups;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import static org.jabref.gui.desktop.os.NativeDesktop.getOtherDataDir;

/**
 * This group contains popular entries that have been viewed more than once.
 * {@link StandardField#GROUPS}.
 */
public class PopularityGroup extends AbstractGroup {
    private static MVStore mvStore;
    private static List<BibEntry> data;
    private final Property<Integer> maxEntriesProperty;
    private final StringProperty timePeriodProperty;

    public PopularityGroup(String name, GroupHierarchyType context, List<BibEntry> entries, Property<Integer> integerProperty, StringProperty textProperty) {
        super(name, context);
        mvStore = getMVStore();
        data = processPopularityGroup(entries, textProperty.getValue(), integerProperty.getValue());
        this.maxEntriesProperty = integerProperty;
        this.timePeriodProperty = textProperty;
    }

    public boolean contains(BibEntry entry) {
        int viewCount = getEntryViewCount(entry);
        int maxEntries = maxEntriesProperty.getValue();
        if (viewCount > 0) {
            for (int i = 0; i < Math.min(maxEntries, data.size()); i++) {
                if (data.get(i).equals(entry)) {
                    return true;
                }
            }
        }
        return false;
    }

        @Override
    public AbstractGroup deepCopy() {
        // Create a copy of this popularity group with the same parameters
        return new PopularityGroup(getName(), getHierarchicalContext(), data, maxEntriesProperty, timePeriodProperty);
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

    public List<BibEntry> processPopularityGroup(List<BibEntry> allEntries, String timePeriodCombo, Integer maxEntriesCombo) {
     List<BibEntry> filteredEntries = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        long timeThreshold = getTimeThreshold(currentTime, timePeriodCombo);

        for (BibEntry entry : allEntries) {
            long lastViewTime = getLastViewTime(entry);
            if (lastViewTime >= timeThreshold) {
                filteredEntries.add(entry);
            }
        }

        filteredEntries.sort((e1, e2) -> Integer.compare(PopularityGroup.getEntryViewCount(e2), PopularityGroup.getEntryViewCount(e1)));

        if (filteredEntries.size() > maxEntriesCombo) {
            filteredEntries = filteredEntries.subList(0, maxEntriesCombo);
        }

        return filteredEntries;
    }

    private long getTimeThreshold(long currentTime, String selectedTimePeriod) {
        return switch (selectedTimePeriod) {
            case "Last week" ->
                    currentTime - 7 * 24 * 60 * 60 * 1000L;
            case "Last month" ->
                    currentTime - 30 * 24 * 60 * 60 * 1000L;
            case "Last year" ->
                    currentTime - 365 * 24 * 60 * 60 * 1000L;
            default -> // "All time"
                    0;
        };
    }

    private long getLastViewTime(BibEntry entry) {
        MVMap<String, Long> lastViewTimestamps = PopularityGroup.getMVStore().openMap("lastViewTimestamps");
        String uniqueKey = PopularityGroup.getUniqueKeyForEntry(entry);
        return lastViewTimestamps.getOrDefault(uniqueKey, 0L);
    }
}
