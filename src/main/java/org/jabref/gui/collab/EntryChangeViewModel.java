package org.jabref.gui.collab;

import java.util.Optional;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.mergeentries.MergeEntriesDialog;
import org.jabref.gui.mergeentries.newmergedialog.ShowDiffConfig;
import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.DiffHighlighter;
import org.jabref.gui.mergeentries.newmergedialog.toolbar.ThreeWayMergeToolbar;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

class EntryChangeViewModel extends DatabaseChangeViewModel {

    private final BibEntry oldEntry;
    private final BibEntry newEntry;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;
    private final ThemeManager themeManager;

    public EntryChangeViewModel(BibEntry entry, BibEntry newEntry, DialogService dialogService, PreferencesService preferencesService,
                                StateManager stateManager, ThemeManager themeManager) {
        super(entry.getCitationKey().map(key -> Localization.lang("Modified entry '%0'", key))
                   .orElse(Localization.lang("Modified entry")));

        this.oldEntry = entry;
        this.newEntry = newEntry;
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.stateManager = stateManager;
        this.themeManager = themeManager;
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        database.getDatabase().removeEntry(oldEntry);
        database.getDatabase().insertEntry(newEntry);
        undoEdit.addEdit(new UndoableInsertEntries(database.getDatabase(), oldEntry));
        undoEdit.addEdit(new UndoableInsertEntries(database.getDatabase(), newEntry));
    }

    @Override
    public Node description() {
        PreviewViewer previewViewer = new PreviewViewer(new BibDatabaseContext(), dialogService, stateManager, themeManager);
        previewViewer.setLayout(preferencesService.getPreviewPreferences().getSelectedPreviewLayout());
        previewViewer.setEntry(newEntry);
        return previewViewer;
    }

    @Override
    public boolean hasAdvancedMergeDialog() {
        return true;
    }

    @Override
    public Optional<DatabaseChangeViewModel> openAdvancedMergeDialog() {
        MergeEntriesDialog mergeEntriesDialog = new MergeEntriesDialog(oldEntry, newEntry);
        mergeEntriesDialog.setLeftHeaderText(Localization.lang("On JabRef"));
        mergeEntriesDialog.setRightHeaderText(Localization.lang("On disk"));
        mergeEntriesDialog.configureDiff(new ShowDiffConfig(ThreeWayMergeToolbar.DiffView.SPLIT, DiffHighlighter.DiffMethod.WORDS));

        return dialogService.showCustomDialogAndWait(mergeEntriesDialog)
                            .map(result -> new EntryChangeViewModel(oldEntry, result.mergedEntry(),
                                    dialogService, preferencesService, stateManager, themeManager));
    }
}
