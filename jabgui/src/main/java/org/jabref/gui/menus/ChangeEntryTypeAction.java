package org.jabref.gui.menus;

import java.util.List;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.UndoManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.change.UndoableChangeType;
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
        undoManager.record(Localization.lang("Change entry type"), recorder ->
                entries.forEach(entry -> {
                    EntryType oldType = entry.getType();
                    if (entry.setType(type).isPresent()) {
                        recorder.record(new UndoableChangeType(entry, oldType, type));
                    }
                }));
    }
}
