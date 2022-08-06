package org.jabref.gui.collab;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

class EntryAddChangeViewModel extends DatabaseChangeViewModel {

    private final BibEntry entry;
    private final PreferencesService preferencesService;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final ThemeManager themeManager;

    public EntryAddChangeViewModel(BibEntry entry,
                                   PreferencesService preferencesService,
                                   DialogService dialogService,
                                   StateManager stateManager,
                                   ThemeManager themeManager) {
        super(entry.getCitationKey()
                   .map(key -> Localization.lang("Added entry '%0'", key))
                   .orElse(Localization.lang("Added entry")));
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.themeManager = themeManager;
        this.stateManager = stateManager;
        this.entry = entry;
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        database.getDatabase().insertEntry(entry);
        undoEdit.addEdit(new UndoableInsertEntries(database.getDatabase(), entry));
    }

    @Override
    public Node description() {
        PreviewViewer previewViewer = new PreviewViewer(new BibDatabaseContext(), dialogService, stateManager, themeManager);
        previewViewer.setLayout(preferencesService.getPreviewPreferences().getSelectedPreviewLayout());
        previewViewer.setEntry(entry);
        return previewViewer;
    }
}
