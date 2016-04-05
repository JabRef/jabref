/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.groups.structure;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.groups.UndoableChangeAssignment;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.QuotedStringTokenizer;
import net.sf.jabref.logic.util.strings.StringUtil;

import javax.swing.undo.AbstractUndoableEdit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Select explicit bibtex entries. It is also known as static group.
 *
 * @author jzieren
 */
public class ExplicitGroup extends AbstractGroup {

    public static final String ID = "ExplicitGroup:";

    private final Set<BibEntry> entries = new HashSet<>();

    public ExplicitGroup(String name, GroupHierarchyType context) {
        super(name, context);
    }

    public static AbstractGroup fromString(String s, BibDatabase db, int version) throws Exception {
        if (!s.startsWith(ExplicitGroup.ID)) {
            throw new Exception(
                    "Internal error: ExplicitGroup cannot be created from \""
                            + s
                            + "\". "
                    + "Please report this on https://github.com/JabRef/jabref/issues");
        }
        QuotedStringTokenizer tok = new QuotedStringTokenizer(s.substring(ExplicitGroup.ID
                .length()), AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR);
        switch (version) {
        case 0:
        case 1:
        case 2: {
            ExplicitGroup newGroup = new ExplicitGroup(tok.nextToken(), GroupHierarchyType.INDEPENDENT);
            newGroup.addEntries(tok, db);
            return newGroup;
        }
        case 3: {
            String name = tok.nextToken();
            int context = Integer.parseInt(tok.nextToken());
            ExplicitGroup newGroup = new ExplicitGroup(name, GroupHierarchyType.getByNumber(context));
            newGroup.addEntries(tok, db);
            return newGroup;
        }
        default:
            throw new UnsupportedVersionException("ExplicitGroup", version);
        }
    }

    /**
     * Called only when created fromString
     */
    private void addEntries(QuotedStringTokenizer tok, BibDatabase db) {
        while (tok.hasMoreTokens()) {
            List<BibEntry> entries = db.getEntriesByKey(StringUtil.unquote(tok.nextToken(), AbstractGroup.QUOTE_CHAR));
            this.entries.addAll(entries);
        }
    }

    @Override
    public boolean supportsAdd() {
        return true;
    }

    @Override
    public boolean supportsRemove() {
        return true;
    }

    @Override
    public AbstractUndoableEdit add(List<BibEntry> entries) {
        if (entries.isEmpty()) {
            return null; // nothing to do
        }

        HashSet<BibEntry> entriesBeforeEdit = new HashSet<>(this.entries);
        this.entries.addAll(entries);

        return new UndoableChangeAssignment(entriesBeforeEdit, this.entries);
    }

    public boolean addEntry(BibEntry entry) {
        return entries.add(entry);
    }

    @Override
    public AbstractUndoableEdit remove(List<BibEntry> entries) {
        if (entries.isEmpty()) {
            return null; // nothing to do
        }

        HashSet<BibEntry> entriesBeforeEdit = new HashSet<>(this.entries);
        for (BibEntry entry : entries) {
            this.entries.remove(entry);
        }

        return new UndoableChangeAssignment(entriesBeforeEdit, this.entries);
    }

    public boolean removeEntry(BibEntry entry) {
        return entries.remove(entry);
    }

    @Override
    public boolean contains(BibEntry entry) {
        return entries.contains(entry);
    }

    @Override
    public AbstractGroup deepCopy() {
        ExplicitGroup copy = new ExplicitGroup(name, context);
        copy.entries.addAll(entries);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ExplicitGroup)) {
            return false;
        }
        ExplicitGroup other = (ExplicitGroup) o;
        // compare entries assigned to both groups
        if (entries.size() != other.entries.size()) {
            return false; // add/remove
        }
        HashSet<String> keys = new HashSet<>();
        BibEntry entry;
        String key;
        // compare bibtex keys for all entries that have one
        for (BibEntry mEntry1 : entries) {
            entry = mEntry1;
            key = entry.getCiteKey();
            if (key != null) {
                keys.add(key);
            }
        }
        for (BibEntry mEntry : other.entries) {
            entry = mEntry;
            key = entry.getCiteKey();
            if ((key != null) && !keys.remove(key)) {
                return false;
            }
        }
        return keys.isEmpty() && other.name.equals(name) && (other.getHierarchicalContext() == getHierarchicalContext());
    }

    /**
     * Returns a String representation of this group and its entries. Entries
     * are referenced by their Bibtexkey. Entries that do not have a Bibtexkey
     * are not included in the representation and will thus not be available
     * upon recreation.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ExplicitGroup.ID).append(StringUtil.quote(name, AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR)).
        append(AbstractGroup.SEPARATOR).append(context.ordinal()).append(AbstractGroup.SEPARATOR);
        String s;
        // write entries in well-defined order for CVS compatibility
        Set<String> sortedKeys = new TreeSet<>();
        for (BibEntry mEntry : entries) {
            s = mEntry.getCiteKey();
            if ((s != null) && !s.isEmpty()) {
                sortedKeys.add(s);
            }
        }
        for (String sortedKey : sortedKeys) {
            sb.append(StringUtil.quote(sortedKey, AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR)).append(AbstractGroup.SEPARATOR);
        }
        return sb.toString();
    }

    /**
     * Remove all assignments, resulting in an empty group.
     */
    public void clearAssignments() {
        entries.clear();
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public String getDescription() {
        return ExplicitGroup.getDescriptionForPreview();
    }

    public static String getDescriptionForPreview() {
        return Localization.lang("This group contains entries based on manual assignment. "
                + "Entries can be assigned to this group by selecting them "
                + "then using either drag and drop or the context menu. "
                + "Entries can be removed from this group by selecting them "
                + "then using the context menu. Every entry assigned to this group "
                + "must have a unique key. The key may be changed at any time "
                + "as long as it remains unique.");
    }

    @Override
    public String getShortDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(getName()).append("</b> -").append(Localization.lang("static group"));
        switch (getHierarchicalContext()) {
        case INCLUDING:
            sb.append(", ").append(Localization.lang("includes subgroups"));
            break;
        case REFINING:
            sb.append(", ").append(Localization.lang("refines supergroup"));
            break;
        default:
            break;
        }
        return sb.toString();
    }

    /**
     * Update the group to handle the situation where the group
     * is applied to a different BibDatabase than it was created for.
     * This group type contains a Set of BibEntry objects, and these will not
     * be the same objects as in the new database. We must reset the entire Set with
     * matching entries from the new database.
     *
     * @param db The database to refresh for.
     */
    @Override
    public void refreshForNewDatabase(BibDatabase db) {
        Set<BibEntry> newSet = new HashSet<>();
        for (BibEntry entry : entries) {
            BibEntry sameEntry = db.getEntryByKey(entry.getCiteKey());
            /*if (sameEntry == null) {
                System.out.println("Error: could not find entry '"+entry.getCiteKey()+"'");
            } else {
                System.out.println("'"+entry.getCiteKey()+"' ok");
            }*/
            newSet.add(sameEntry);
        }
        entries.clear();
        entries.addAll(newSet);
    }

    public Set<BibEntry> getEntries() {
        return entries;
    }

    @Override
    public String getTypeId() {
        return ExplicitGroup.ID;
    }

    public int getNumEntries() {
        return entries.size();
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return super.hashCode();
    }

}
