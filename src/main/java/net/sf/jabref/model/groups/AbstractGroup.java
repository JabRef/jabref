package net.sf.jabref.model.groups;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.search.SearchMatcher;

/**
 * A group of BibtexEntries.
 */
public abstract class AbstractGroup implements SearchMatcher {

    /**
     * Character used for quoting in the string representation.
     */
    public static final char QUOTE_CHAR = '\\';
    /**
     * For separating units (e.g. name, which every group has) in the string
     * representation
     */
    public static final String SEPARATOR = ";";
    /**
     * The group's name (every type of group has one).
     */
    private String name;
    /**
     * The hierarchical context of the group (INDEPENDENT, REFINING, or
     * INCLUDING). Defaults to INDEPENDENT, which will be used if and
     * only if the context specified in the constructor is invalid.
     */
    private GroupHierarchyType context = GroupHierarchyType.INDEPENDENT;


    protected AbstractGroup(String name, GroupHierarchyType context) {
        this.name = name;
        setHierarchicalContext(context);
    }

    public GroupHierarchyType getContext() {
        return context;
    }

    public abstract String getTypeId();

    /**
     * Returns this group's name, e.g. for display in a list/tree.
     */
    public final String getName() {
        return name;
    }

    /**
     * Sets the group's name.
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * @return true if this type of group supports the explicit adding of
     * entries.
     */
    public abstract boolean supportsAdd();

    /**
     * @return true if this type of group supports the explicit removal of
     * entries.
     */
    public abstract boolean supportsRemove();

    /**
     * Adds the specified entries to this group.
     *
     * @return If this group or one or more entries was/were modified as a
     * result of this operation, an object is returned that allows to
     * undo this change. null is returned otherwise.
     */
    public abstract Optional<EntriesGroupChange> add(List<BibEntry> entriesToAdd);

    public Optional<EntriesGroupChange> add(BibEntry entryToAdd) {
        return add(Collections.singletonList(entryToAdd));
    }

    /**
     * Removes the specified entries from this group.
     *
     * @return If this group or one or more entries was/were modified as a
     * result of this operation, an object is returned that allows to
     * undo this change. null is returned otherwise.
     */
    public abstract Optional<EntriesGroupChange> remove(List<BibEntry> entriesToRemove);

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
     * Sets the groups's hierarchical context. If context is not a valid
     * value, the call is ignored.
     */
    public void setHierarchicalContext(GroupHierarchyType context) {
        if (context == null) {
            return;
        }
        this.context = context;
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
