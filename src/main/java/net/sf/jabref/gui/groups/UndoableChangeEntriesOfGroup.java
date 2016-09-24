package net.sf.jabref.gui.groups;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.groups.EntriesGroupChange;

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
