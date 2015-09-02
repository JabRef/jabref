/*  Copyright (C) 2003-2011 JabRef contributors.
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

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.search.SearchRule;

/**
 * This group contains all entries. Always. At any time!
 */
public class AllEntriesGroup extends AbstractGroup {

    public static final String ID = "AllEntriesGroup:";

    public AllEntriesGroup() {
        super(Globals.lang("All Entries"), GroupHierarchyType.INDEPENDENT);
    }

    public static AbstractGroup fromString(String s, BibtexDatabase db, int version) throws Exception {
        if (!s.startsWith(AllEntriesGroup.ID)) {
            throw new Exception(
                    "Internal error: AllEntriesGroup cannot be created from \""
                            + s + "\". "
                            + "Please report this on www.sf.net/projects/jabref");
        }
        switch (version) {
        case 0:
        case 1:
        case 2:
        case 3:
            return new AllEntriesGroup();
        default:
            throw new UnsupportedVersionException("AllEntriesGroup", version);
        }
    }

    @Override
    public SearchRule getSearchRule() {
        return new SearchRule() {
            @Override
            public boolean applyRule(String query, BibtexEntry bibtexEntry) {
                return true; // contains everything
            }

            @Override
            public boolean validateSearchStrings(String query) {
                return true;
            }
        };
    }

    @Override
    public boolean supportsAdd() {
        return false;
    }

    @Override
    public boolean supportsRemove() {
        return false;
    }

    @Override
    public AbstractUndoableEdit add(BibtexEntry[] entries) {
        // not supported -> ignore
        return null;
    }

    @Override
    public AbstractUndoableEdit remove(BibtexEntry[] entries) {
        // not supported -> ignore
        return null;
    }

    @Override
    public boolean contains(String query, BibtexEntry entry) {
        return true; // contains everything
    }

    @Override
    public AbstractGroup deepCopy() {
        return new AllEntriesGroup();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AllEntriesGroup;
    }

    @Override
    public String toString() {
        return AllEntriesGroup.ID;
    }

    @Override
    public boolean contains(BibtexEntry entry) {
        return true;
    }

    @Override
    public boolean isDynamic() {
        // this is actually a special case; I define it as non-dynamic
        return false;
    }

    @Override
    public String getDescription() {
        return Globals.lang("This group contains all entries. It cannot be edited or removed.");
    }

    @Override
    public String getShortDescription() {
        return Globals.lang("<b>All Entries</b> (this group cannot be edited or removed)");
    }

    @Override
    public String getTypeId() {
        return AllEntriesGroup.ID;
    }
}
