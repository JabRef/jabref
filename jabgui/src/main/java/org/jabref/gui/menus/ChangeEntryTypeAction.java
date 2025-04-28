package org.jabref.gui.menus;

import java.util.List;

import javax.swing.undo.UndoManager;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableChangeType;
import org.jabref.logic.l10n.Localization;
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
        NamedCompound compound = new NamedCompound(Localization.lang("Change entry type"));
        entries.forEach(e -> e.setType(type)
                              .ifPresent(change -> compound.addEdit(new UndoableChangeType(change))));
        undoManager.addEdit(compound);
    }
}
