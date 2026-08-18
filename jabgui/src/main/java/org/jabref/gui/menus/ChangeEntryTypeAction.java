package org.jabref.gui.menus;

import java.util.List;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.change.EntryTypeEdit;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;

public class ChangeEntryTypeAction extends SimpleCommand {

    private final EntryType type;
    private final List<BibEntry> entries;
    private final UndoManager undoManager;

    public ChangeEntryTypeAction(EntryType type, List<BibEntry> entries, UndoManager undoManager) {
        this.type = type;
        this.entries = entries;
        this.undoManager = undoManager;
    }

    @Override
    public void execute() {
        NamedCompoundEdit compound = new NamedCompoundEdit(Localization.lang("Change entry type"));
        entries.forEach(entry -> {
            EntryType oldType = entry.getType();
            if (entry.setType(type).isPresent()) {
                compound.addEdit(new EntryTypeEdit(entry, oldType, type));
            }
        });
        undoManager.push(compound.toChangeSet());
    }
}
