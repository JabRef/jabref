package org.jabref.gui.fieldeditors.identifier;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ISBN;

public class ISBNIdentifierEditorViewModel extends BaseIdentifierEditorViewModel<ISBN> {
    private final UndoManager undoManager;
    private final StateManager stateManager;

    public ISBNIdentifierEditorViewModel(SuggestionProvider<?> suggestionProvider,
                                         FieldCheckers fieldCheckers,
                                         DialogService dialogService,
                                         TaskExecutor taskExecutor,
                                         GuiPreferences preferences,
                                         UndoManager undoManager,
                                         StateManager stateManager) {
        super(StandardField.ISBN, suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferences, undoManager);
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        configure(true, false, false);
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
