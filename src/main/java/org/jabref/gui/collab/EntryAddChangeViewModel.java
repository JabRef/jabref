package org.jabref.gui.collab;

import javafx.scene.Node;

import org.jabref.gui.JabRefGUI;
import org.jabref.gui.StateManager;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

class EntryAddChangeViewModel extends DatabaseChangeViewModel {

    private final BibEntry entry;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;

    public EntryAddChangeViewModel(BibEntry entry, PreferencesService preferencesService, StateManager stateManager) {
        super();
        this.preferencesService = preferencesService;
        this.stateManager = stateManager;
        this.name = entry.getCitationKey()
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
        PreviewViewer previewViewer = new PreviewViewer(new BibDatabaseContext(), JabRefGUI.getMainFrame().getDialogService(), stateManager);
        previewViewer.setLayout(preferencesService.getPreviewPreferences().getCurrentPreviewStyle());
        previewViewer.setEntry(entry);
        return previewViewer;
    }
}
