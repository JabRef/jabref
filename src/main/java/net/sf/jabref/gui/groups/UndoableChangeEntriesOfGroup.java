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
package net.sf.jabref.gui.groups;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.groups.EntriesGroupChange;
import net.sf.jabref.logic.l10n.Localization;

public class UndoableChangeEntriesOfGroup {

   public static AbstractUndoableEdit getUndoableEdit(GroupTreeNodeViewModel node, EntriesGroupChange change) {

       if(change.getOldEntries().size() != change.getNewEntries().size()) {
           return new UndoableChangeAssignment(node, change.getOldEntries(), change.getNewEntries());
       }

       boolean hasEntryChanges = false;
       NamedCompound entryChangeCompound = new NamedCompound(Localization.lang("change entries of group"));
       for(FieldChange fieldChange : change.getEntryChanges()) {
           hasEntryChanges = true;
           entryChangeCompound.addEdit(new UndoableFieldChange(fieldChange));
       }
       if (hasEntryChanges) {
           entryChangeCompound.end();
           return entryChangeCompound;
       }

       return null;
   }
}
