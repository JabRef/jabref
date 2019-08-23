package org.jabref.gui.collab;

import javafx.scene.Node;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

class EntryAddChangeViewModel extends DatabaseChangeViewModel {

    private final BibEntry diskEntry;

    public EntryAddChangeViewModel(BibEntry diskEntry) {
        super(Localization.lang("Added entry"));
        this.diskEntry = diskEntry;
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        database.getDatabase().insertEntry(diskEntry);
        undoEdit.addEdit(new UndoableInsertEntry(database.getDatabase(), diskEntry));
    }

    @Override
    public Node description() {
        PreviewViewer previewViewer = new PreviewViewer(new BibDatabaseContext(), JabRefGUI.getMainFrame().getDialogService(), Globals.stateManager);
        previewViewer.setEntry(diskEntry);
        return previewViewer;
    }
}
