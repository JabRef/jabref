package net.sf.jabref.groups;

import java.util.*;

import net.sf.jabref.*;

/**
 * A group of BibtexEntries.
 */
public abstract class AbstractGroup {
    /** Character used for quoting in the string representation. */
    protected static final char QUOTE_CHAR = '\\';
    /**
     * For separating units (e.g. name, which every group has) in the string
     * representation
     */
    protected static final String SEPARATOR = ";";

    /**
     * @return A search rule that will identify this group's entries.
     */
    public abstract SearchRule getSearchRule();

    /**
     * Re-create a group instance.
     * 
     * @param s
     *            The result from the group's toString() method.
     * @return New instance of the encoded group.
     * @throws Exception
     *             If an error occured and a group could not be created, e.g.
     *             due to a malformed regular expression.
     */
    public static AbstractGroup fromString(String s) throws Exception {
        if (s.startsWith(KeywordGroup.ID))
            return KeywordGroup.fromString(s);
        if (s.startsWith(AllEntriesGroup.ID))
            return AllEntriesGroup.fromString(s);
        if (s.startsWith(SearchGroup.ID))
            return SearchGroup.fromString(s);
        if (s.startsWith(ExplicitGroup.ID))
            return ExplicitGroup.fromString(s);
        return null; // unknown group
    }

    /** Returns this group's name, e.g. for display in a list/tree. */
    public abstract String getName();

    /**
     * Re-create multiple instances (of not necessarily the same type) from the
     * specified Vector.
     * 
     * @param vector
     *            A vector containing String representations obtained from a
     *            group's toString() method.
     * @return A vector containing the recreated group instances.
     * @throws Exception
     *             If an error occured and a group could not be created, e.g.
     *             due to a malformed regular expression.
     */
    public static final Vector fromString(Vector vector) throws Exception {
        Vector groups = new Vector();
        for (int i = 0; i < vector.size(); ++i)
            groups.add(fromString(vector.elementAt(i).toString()));
        return groups;
    }

    /**
     * @return true if this type of group supports the explicit adding of
     *         entries.
     */
    public abstract boolean supportsAdd();

    /**
     * @return true if this type of group supports the explicit removal of
     *         entries.
     */
    public abstract boolean supportsRemove();

    /**
     * Adds the selected entries to this group.
     */
    public abstract void addSelection(BasePanel basePanel);

    /**
     * Removes the selected entries from this group.
     */
    public abstract void removeSelection(BasePanel basePanel);

    /**
     * @return A value >0 if this group contains the specified entry, 0
     *         otherwise.
     */
    public abstract int contains(Map searchOptions, BibtexEntry entry);

    /**
     * @return A deep copy of this object.
     */
    public abstract AbstractGroup deepCopy();

    // by general AbstractGroup contract, toString() must return
    // something from which this object can be reconstructed
    // using fromString(String).

    // by general AbstractGroup contract, equals() must be implemented
}
