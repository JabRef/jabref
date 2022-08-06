package org.jabref.gui.collab;

import javafx.scene.Node;

import org.jabref.gui.Globals;
import org.jabref.gui.JabRefGUI;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EntryDeleteChangeViewModel extends DatabaseChangeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryDeleteChangeViewModel.class);
    private final BibEntry entry;

    private final PreferencesService preferencesService;

    public EntryDeleteChangeViewModel(BibEntry entry, PreferencesService preferencesService) {
        super(entry.getCitationKey()
                   .map(key -> Localization.lang("Deleted entry") + ": '" + key + '\'')
                   .orElse(Localization.lang("Deleted entry")));
        this.preferencesService = preferencesService;
        this.entry = entry;
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        database.getDatabase().removeEntry(entry);
        undoEdit.addEdit(new UndoableRemoveEntries(database.getDatabase(), entry));
    }

    @Override
    public Node description() {
        PreviewViewer previewViewer = new PreviewViewer(new BibDatabaseContext(), JabRefGUI.getMainFrame().getDialogService(), Globals.stateManager, Globals.getThemeManager());
        previewViewer.setLayout(preferencesService.getPreviewPreferences().getSelectedPreviewLayout());
        previewViewer.setEntry(entry);
        return previewViewer;
    }
}
