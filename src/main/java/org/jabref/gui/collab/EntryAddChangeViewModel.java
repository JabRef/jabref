package org.jabref.gui.collab;

import javafx.scene.Node;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

class EntryAddChangeViewModel extends DatabaseChangeViewModel {

    private final BibEntry entry;

    public EntryAddChangeViewModel(BibEntry entry) {
        super();
        this.name = entry.getCiteKeyOptional()
                         .map(key -> Localization.lang("Added entry") + ": '" + key + '\'')
                         .orElse(Localization.lang("Added entry"));
        this.entry = entry;
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        database.getDatabase().insertEntry(entry);
        undoEdit.addEdit(new UndoableInsertEntries(database.getDatabase(), entry));
    }

    @Override
    public Node description() {
        PreviewViewer previewViewer = new PreviewViewer(new BibDatabaseContext(), JabRefGUI.getMainFrame().getDialogService(), Globals.stateManager);
        previewViewer.setLayout(Globals.prefs.getPreviewPreferences().getCurrentPreviewStyle());
        previewViewer.setEntry(entry);
        return previewViewer;
    }
}
