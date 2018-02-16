package org.jabref.gui.groups;

import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;

public class UndoableChangeEntriesOfGroup {

    private UndoableChangeEntriesOfGroup() {
    }

    public static AbstractUndoableEdit getUndoableEdit(GroupTreeNodeViewModel node, List<FieldChange> changes) {
        boolean hasEntryChanges = false;
        NamedCompound entryChangeCompound = new NamedCompound(Localization.lang("change entries of group"));
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
