package net.sf.jabref.groups;

import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.*;

/**
 * A group of BibtexEntries.
 */
public abstract class AbstractGroup {
    protected String m_name;
    
    public AbstractGroup(String name) {
        m_name = name;
    }
    
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
    public static AbstractGroup fromString(String s, BibtexDatabase db)
            throws Exception {
        if (s.startsWith(KeywordGroup.ID))
            return KeywordGroup.fromString(s);
        if (s.startsWith(AllEntriesGroup.ID))
            return AllEntriesGroup.fromString(s);
        if (s.startsWith(SearchGroup.ID))
            return SearchGroup.fromString(s);
        if (s.startsWith(ExplicitGroup.ID))
            return ExplicitGroup.fromString(s, db);
        return null; // unknown group
    }

    /** Returns this group's name, e.g. for display in a list/tree. */
    public final String getName() {
        return m_name;
    }
    
    /** Sets the group's name. */
    public final void setName(String name) {
        m_name = name;
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
     * 
     * @return If this group or one or more entries was/were modified as a
     *         result of this operation, an object is returned that allows to
     *         undo this change. null is returned otherwise.
     */
    public abstract AbstractUndoableEdit addSelection(BasePanel basePanel);

    /**
     * Removes the selected entries from this group.
     * 
     * @return If this group or one or more entries was/were modified as a
     *         result of this operation, an object is returned that allows to
     *         undo this change. null is returned otherwise.
     */
    public abstract AbstractUndoableEdit removeSelection(BasePanel basePanel);

    /**
     * @return true if this group contains the specified entry, false otherwise.
     */
    public abstract boolean contains(Map searchOptions, BibtexEntry entry);

    /**
     * @return A deep copy of this object.
     */
    public abstract AbstractGroup deepCopy();

    // by general AbstractGroup contract, toString() must return
    // something from which this object can be reconstructed
    // using fromString(String).

    // by general AbstractGroup contract, equals() must be implemented
}
