package org.jabref.gui.groups;

import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;

import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;

public class UndoableChangeEntriesOfGroup {

    private UndoableChangeEntriesOfGroup() {
    }

    public static AbstractUndoableEdit getUndoableEdit(GroupTreeNodeViewModel node, List<FieldChange> changes) {
        boolean hasEntryChanges = false;
        NamedCompoundEdit entryChangeCompound = new NamedCompoundEdit(Localization.lang("change entries of group"));
        for (FieldChange fieldChange : changes) {
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
