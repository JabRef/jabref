package org.jabref.gui.menus;

import java.util.List;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.undo.UndoManager;
import org.jabref.model.undo.UndoableChangeType;

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
        undoManager.addEdit(Localization.lang("Change entry type"), edit ->
                entries.forEach(entry -> {
                    EntryType oldType = entry.getType();
                    if (entry.setType(type).isPresent()) {
                        edit.addEdit(new UndoableChangeType(entry, oldType, type));
                    }
                }));
    }
}
