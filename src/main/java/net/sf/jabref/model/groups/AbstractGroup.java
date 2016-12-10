package net.sf.jabref.model.groups;

import java.util.List;
import java.util.Objects;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.search.SearchMatcher;

/**
 * Base class for all groups.
 */
public abstract class AbstractGroup implements SearchMatcher {

    /**
     * The group's name.
     */
    protected final String name;

    /**
     * The hierarchical context of the group.
     */
    protected final GroupHierarchyType context;

    protected AbstractGroup(String name, GroupHierarchyType context) {
        this.name = name;
        this.context = Objects.requireNonNull(context);
    }

    /**
     * Returns the way this group relates to its sub- or supergroup.
     */
    public GroupHierarchyType getHierarchicalContext() {
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
     * @return true if this group contains any of the specified entries, false otherwise.
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
     * @return true if this group contains all of the specified entries, false otherwise.
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
     * @return A deep copy of this object.
     */
    public abstract AbstractGroup deepCopy();
}
