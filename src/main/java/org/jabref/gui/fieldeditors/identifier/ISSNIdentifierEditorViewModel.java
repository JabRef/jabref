package org.jabref.gui.fieldeditors.identifier;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ISSN;
import org.jabref.preferences.PreferencesService;

public class ISSNIdentifierEditorViewModel extends BaseIdentifierEditorViewModel<ISSN> {
    private final StateManager stateManager;
    private final UndoManager undoManager;

    public ISSNIdentifierEditorViewModel(SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, DialogService dialogService, TaskExecutor taskExecutor, PreferencesService preferences, UndoManager undoManager, StateManager stateManager) {
        super(StandardField.ISBN, suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferences, undoManager);
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        configure(true, false);
    }

    @Override
    public void fetchBibliographyInformation(BibEntry bibEntry) {
        stateManager.getActiveDatabase().ifPresentOrElse(
                databaseContext -> new FetchAndMergeEntry(databaseContext, taskExecutor, preferences, dialogService, undoManager)
                        .fetchAndMerge(entry, field),
                () -> dialogService.notify(Localization.lang("No library selected"))
        );
    }
}
