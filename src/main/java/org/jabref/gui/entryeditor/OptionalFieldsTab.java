package org.jabref.gui.entryeditor;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.PreferencesService;

import javax.swing.undo.UndoManager;

public class OptionalFieldsTab extends OptionalFieldsTabBase {
    public OptionalFieldsTab(BibDatabaseContext databaseContext,
                             SuggestionProviders suggestionProviders,
                             UndoManager undoManager,
                             DialogService dialogService,
                             PreferencesService preferences,
                             StateManager stateManager,
                             BibEntryTypesManager entryTypesManager,
                             ExternalFileTypes externalFileTypes,
                             TaskExecutor taskExecutor,
                             JournalAbbreviationRepository journalAbbreviationRepository) {
        super(
                "Optional fields",
                true,
                databaseContext,
                suggestionProviders,
                undoManager,
                dialogService,
                preferences,
                stateManager,
                entryTypesManager,
                externalFileTypes,
                taskExecutor,
                journalAbbreviationRepository
        );
    }
}
