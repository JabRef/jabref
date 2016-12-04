package net.sf.jabref.model.groups;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.search.SearchMatcher;

/**
 * Base class for all groups.
 */
public abstract class AbstractGroup implements SearchMatcher {

    /**
     * The group's name.
     */
    private final String name;
    /**
     * The hierarchical context of the group.
     */
    private final GroupHierarchyType context;

    protected AbstractGroup(String name, GroupHierarchyType context) {
        this.name = name;
        this.context = Objects.requireNonNull(context);
    }

    public GroupHierarchyType getContext() {
        return context;
    }

    /**
     * Returns this group's name, e.g. for display in a list/tree.
     */
    public final String getName() {
        return name;
    }

    /**
     * @return true if this group contains the specified entry, false otherwise.
     */
    public abstract boolean contains(BibEntry entry);

    @Override
    public boolean isMatch(BibEntry entry) {
        return contains(entry);
    }

    /**
     * @return true if this group contains any of the specified entries, false
     * otherwise.
     */
    public boolean containsAny(List<BibEntry> entries) {
        for (BibEntry entry : entries) {
            if (contains(entry)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if this group contains all of the specified entries, false
     * otherwise.
     */
    public boolean containsAll(List<BibEntry> entries) {
        for (BibEntry entry : entries) {
            if (!contains(entry)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if this group is dynamic, i.e. uses a search definition or
     * equiv. that might match new entries, or false if this group contains a
     * fixed set of entries and thus will never match a new entry that was not
     * explicitly added to it.
     */
    public abstract boolean isDynamic();

    /**
     * Returns the group's hierarchical context.
     */
    public GroupHierarchyType getHierarchicalContext() {
        return context;
    }

    /**
     * @return A deep copy of this object.
     */
    public abstract AbstractGroup deepCopy();

    // by general AbstractGroup contract, toString() must return
    // something from which this object can be reconstructed
    // using fromString(String).

    // by general AbstractGroup contract, equals() must be implemented

    /**
     * Update the group, if necessary, to handle the situation where the group
     * is applied to a different BibDatabase than it was created for. This
     * is for instance used when updating the group tree due to an external change.
     *
     * @param db The database to refresh for.
     */
    public void refreshForNewDatabase(BibDatabase db) {
        // Default is to do nothing. Group types that are affected by a change
        // of database must override this method.
    }
}
