package org.jabref.gui.menus;

import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

import org.jabref.gui.EntryTypeView;
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
    private final ReadOnlyStringWrapper statusMessageProperty;

    public ChangeEntryTypeAction(EntryType type, List<BibEntry> entries, UndoManager undoManager) {
        this.type = type;
        this.entries = entries;
        this.undoManager = undoManager;
        this.statusMessageProperty = new ReadOnlyStringWrapper(EntryTypeView.getDescription(type));
    }

    @Override
    public void execute() {
        NamedCompound compound = new NamedCompound(Localization.lang("Change entry type"));
        entries.forEach(e -> e.setType(type)
                              .ifPresent(change -> compound.addEdit(new UndoableChangeType(change))));
        undoManager.addEdit(compound);
    }

    @Override
    public String getStatusMessage() {
        return statusMessage.get();
    }

    @Override
    public ReadOnlyStringProperty statusMessageProperty() {
        return statusMessageProperty.getReadOnlyProperty();
    }
}
