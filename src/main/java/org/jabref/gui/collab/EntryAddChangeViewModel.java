package org.jabref.gui.collab;

import javafx.scene.Node;

import org.jabref.Globals;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.PreviewPanel;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
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
        PreviewPanel previewPanel = new PreviewPanel(null, null, Globals.getKeyPrefs(), Globals.prefs.getPreviewPreferences(), new FXDialogService(), ExternalFileTypes.getInstance());
        previewPanel.setEntry(diskEntry);
        return previewPanel;
    }
}
