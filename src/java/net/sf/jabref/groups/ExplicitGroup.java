package net.sf.jabref.groups;

import java.util.*;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.*;
import net.sf.jabref.util.QuotedStringTokenizer;

/**
 * @author zieren
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class ExplicitGroup extends AbstractGroup implements SearchRule {
    public static final String ID = "ExplicitGroup:";
    private final Set m_entries;
    private final BibtexDatabase m_database;

    public ExplicitGroup(String name, BibtexDatabase db) {
        super(name);
        m_entries = new HashSet();
        m_database = db;
    }

    public static AbstractGroup fromString(String s, BibtexDatabase db)
            throws Exception {
        if (!s.startsWith(ID))
            throw new Exception(
                    "Internal error: ExplicitGroup cannot be created from \""
                            + s + "\"");
        QuotedStringTokenizer tok = new QuotedStringTokenizer(s.substring(ID
                .length()), SEPARATOR, QUOTE_CHAR);
        ExplicitGroup newGroup = new ExplicitGroup(tok.nextToken(), db);
        BibtexEntry[] entries;
        while (tok.hasMoreTokens()) {
            entries = db.getEntriesByKey(Util.unquote(tok.nextToken(),
                    QUOTE_CHAR));
            for (int i = 0; i < entries.length; ++i)
                newGroup.m_entries.add(entries[i]);
        }
        return newGroup;
    }

    public SearchRule getSearchRule() {
        return this;
    }

    public boolean supportsAdd() {
        return true;
    }

    public boolean supportsRemove() {
        return true;
    }

    public AbstractUndoableEdit addSelection(BasePanel basePanel) {
        BibtexEntry[] bes = basePanel.getSelectedEntries();
        if (bes.length == 0)
            return null; // nothing to do

        HashSet entriesBeforeEdit = new HashSet(m_entries);
        for (int i = 0; i < bes.length; ++i)
            m_entries.add(bes[i]);

        return new UndoableChangeAssignment(entriesBeforeEdit, m_entries);
    }
    
    public boolean addEntry(BibtexEntry entry) {
        return m_entries.add(entry);
    }

    public AbstractUndoableEdit removeSelection(BasePanel basePanel) {
        BibtexEntry[] bes = basePanel.getSelectedEntries();
        if (bes.length == 0)
            return null; // nothing to do

        HashSet entriesBeforeEdit = new HashSet(m_entries);
        for (int i = 0; i < bes.length; ++i)
            m_entries.remove(bes[i]);

        return new UndoableChangeAssignment(entriesBeforeEdit, m_entries);
    }

    public boolean removeEntry(BibtexEntry entry) {
        return m_entries.remove(entry);
    }

    public boolean contains(BibtexEntry entry) {
        return m_entries.contains(entry);
    }

    public boolean contains(Map searchOptions, BibtexEntry entry) {
        return contains(entry);
    }

    public int applyRule(Map searchStrings, BibtexEntry bibtexEntry) {
        return contains(searchStrings, bibtexEntry) ? 1 : 0;
    }

    public AbstractGroup deepCopy() {
        ExplicitGroup copy = new ExplicitGroup(m_name, m_database);
        copy.m_entries.addAll(m_entries);
        return copy;
    }

    public boolean equals(Object o) {
        if (!(o instanceof ExplicitGroup))
            return false;
        ExplicitGroup other = (ExplicitGroup) o;
        return other.m_name.equals(m_name) && other.m_entries.equals(m_entries)
                && other.m_database == m_database;
    }

    /**
     * Returns a String representation of this group and its entries. Entries
     * are referenced by their Bibtexkey. Entries that do not have a Bibtexkey
     * are not included in the representation and will thus not be available
     * upon recreation.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(ID + Util.quote(m_name, SEPARATOR, QUOTE_CHAR) + SEPARATOR);
        String s;
        for (Iterator it = m_entries.iterator(); it.hasNext();) {
            s = ((BibtexEntry) it.next()).getCiteKey();
            if (s != null && !s.equals(""))
                sb.append(Util.quote(s, SEPARATOR, QUOTE_CHAR) + SEPARATOR);
        }
        return sb.toString();
    }
}
