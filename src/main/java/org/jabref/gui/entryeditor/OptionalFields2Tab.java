package org.jabref.gui.entryeditor;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.search.indexing.IndexingTaskManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.PreferencesService;

public class OptionalFields2Tab extends OptionalFieldsTabBase {
    public OptionalFields2Tab(BibDatabaseContext databaseContext,
                              SuggestionProviders suggestionProviders,
                              UndoManager undoManager,
                              DialogService dialogService,
                              PreferencesService preferences,
                              StateManager stateManager,
                              ThemeManager themeManager,
                              IndexingTaskManager indexingTaskManager,
                              BibEntryTypesManager entryTypesManager,
                              TaskExecutor taskExecutor,
                              JournalAbbreviationRepository journalAbbreviationRepository) {
        super(
                Localization.lang("Optional fields 2"),
                false,
                databaseContext,
                suggestionProviders,
                undoManager,
                dialogService,
                preferences,
                stateManager,
                themeManager,
                indexingTaskManager,
                entryTypesManager,
                taskExecutor,
                journalAbbreviationRepository
        );
    }
}
