package org.jabref.gui.groups;

import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;

import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.change.FieldEdit;

public class UndoableChangeEntriesOfGroup {

    private UndoableChangeEntriesOfGroup() {
    }

    public static AbstractUndoableEdit getUndoableEdit(GroupTreeNodeViewModel node, List<FieldChange> changes) {
        boolean hasEntryChanges = false;
        NamedCompoundEdit entryChangeCompound = new NamedCompoundEdit(Localization.lang("change entries of group"));
        for (FieldChange fieldChange : changes) {
            hasEntryChanges = true;
            entryChangeCompound.addEdit(new FieldEdit(fieldChange));
        }
        if (hasEntryChanges) {
            return entryChangeCompound;
        }
        return null;
    }
}
