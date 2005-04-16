/*
All programs in this directory and subdirectories are published under the 
GNU General Public License as described below.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by the Free 
Software Foundation; either version 2 of the License, or (at your option) 
any later version.

This program is distributed in the hope that it will be useful, but WITHOUT 
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
more details.

You should have received a copy of the GNU General Public License along 
with this program; if not, write to the Free Software Foundation, Inc., 59 
Temple Place, Suite 330, Boston, MA 02111-1307 USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html
*/

package net.sf.jabref.groups;

import java.util.*;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.*;
import net.sf.jabref.util.QuotedStringTokenizer;

/**
 * @author jzieren
 * 
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

    public static AbstractGroup fromString(String s, BibtexDatabase db, int version)
            throws Exception {
        if (!s.startsWith(ID))
            throw new Exception(
                    "Internal error: ExplicitGroup cannot be created from \""
                            + s + "\"");
        QuotedStringTokenizer tok = new QuotedStringTokenizer(s.substring(ID
                .length()), SEPARATOR, QUOTE_CHAR);
        switch (version) {
        case 0:
        case 1:
            ExplicitGroup newGroup = new ExplicitGroup(tok.nextToken(), db);
            BibtexEntry[] entries;
            while (tok.hasMoreTokens()) {
                entries = db.getEntriesByKey(Util.unquote(tok.nextToken(),
                        QUOTE_CHAR));
                for (int i = 0; i < entries.length; ++i)
                    newGroup.m_entries.add(entries[i]);
            }
            return newGroup;
        default:
            throw new UnsupportedVersionException("ExplicitGroup", version);
        }
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

    public AbstractUndoableEdit addSelection(BibtexEntry[] entries) {
        if (entries.length == 0)
            return null; // nothing to do

        HashSet entriesBeforeEdit = new HashSet(m_entries);
        for (int i = 0; i < entries.length; ++i)
            m_entries.add(entries[i]);

        return new UndoableChangeAssignment(entriesBeforeEdit, m_entries);
    }
    
    public AbstractUndoableEdit addSelection(BasePanel basePanel) {
        return addSelection(basePanel.getSelectedEntries());
    }
    
    public boolean addEntry(BibtexEntry entry) {
        return m_entries.add(entry);
    }

    public AbstractUndoableEdit removeSelection(BibtexEntry[] entries) {
        if (entries.length == 0)
            return null; // nothing to do

        HashSet entriesBeforeEdit = new HashSet(m_entries);
        for (int i = 0; i < entries.length; ++i)
            m_entries.remove(entries[i]);

        return new UndoableChangeAssignment(entriesBeforeEdit, m_entries);
    }

    public AbstractUndoableEdit removeSelection(BasePanel basePanel) {
        return removeSelection(basePanel.getSelectedEntries());
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
    
    /** Remove all assignments, resulting in an empty group. */
    public void clearAssignments() {
        m_entries.clear();
    }
}
