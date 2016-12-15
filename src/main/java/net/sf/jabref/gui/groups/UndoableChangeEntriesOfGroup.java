package net.sf.jabref.gui.groups;

import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.FieldChange;

public class UndoableChangeEntriesOfGroup {

   public static AbstractUndoableEdit getUndoableEdit(GroupTreeNodeViewModel node, List<FieldChange> changes) {
       boolean hasEntryChanges = false;
       NamedCompound entryChangeCompound = new NamedCompound(Localization.lang("change entries of group"));
       for(FieldChange fieldChange : changes) {
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
