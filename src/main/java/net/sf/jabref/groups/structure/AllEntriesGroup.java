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

import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.logic.l10n.Localization;

/**
 * This group contains all entries. Always. At any time!
 */
public class AllEntriesGroup extends AbstractGroup {

    public static final String ID = "AllEntriesGroup:";

    public AllEntriesGroup() {
        super(Localization.lang("All entries"), GroupHierarchyType.INDEPENDENT);
    }

    public static AbstractGroup fromString(String s, BibDatabase db, int version) throws Exception {
        if (!s.startsWith(AllEntriesGroup.ID)) {
            throw new Exception(
                    "Internal error: AllEntriesGroup cannot be created from \""
                            + s + "\". "
                    + "Please report this on https://github.com/JabRef/jabref/issues");
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
    public boolean supportsAdd() {
        return false;
    }

    @Override
    public boolean supportsRemove() {
        return false;
    }

    @Override
    public AbstractUndoableEdit add(List<BibEntry> entries) {
        // not supported -> ignore
        return null;
    }

    @Override
    public AbstractUndoableEdit remove(List<BibEntry> entries) {
        // not supported -> ignore
        return null;
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
    public boolean contains(BibEntry entry) {
        return true;
    }

    @Override
    public boolean isDynamic() {
        // this is actually a special case; I define it as non-dynamic
        return false;
    }

    @Override
    public String getDescription() {
        return Localization.lang("This group contains all entries. It cannot be edited or removed.");
    }

    @Override
    public String getShortDescription() {
        return Localization.lang("<b>All Entries</b> (this group cannot be edited or removed)");
    }

    @Override
    public String getTypeId() {
        return AllEntriesGroup.ID;
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return super.hashCode();
    }
}
