package org.jabref.gui.entryeditor;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.LuceneManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.search.SearchQuery;
import org.jabref.preferences.PreferencesService;

public class DetailOptionalFieldsTab extends OptionalFieldsTabBase {

    public static final String NAME = "Optional fields 2";

    public DetailOptionalFieldsTab(BibDatabaseContext databaseContext,
                                   SuggestionProviders suggestionProviders,
                                   UndoManager undoManager,
                                   UndoAction undoAction,
                                   RedoAction redoAction,
                                   DialogService dialogService,
                                   PreferencesService preferences,
                                   ThemeManager themeManager,
                                   BibEntryTypesManager entryTypesManager,
                                   TaskExecutor taskExecutor,
                                   JournalAbbreviationRepository journalAbbreviationRepository,
                                   LuceneManager luceneManager,
                                   OptionalObjectProperty<SearchQuery> searchQueryProperty) {
        super(
                Localization.lang("Optional fields 2"),
                false,
                databaseContext,
                suggestionProviders,
                undoManager,
                undoAction,
                redoAction,
                dialogService,
                preferences,
                themeManager,
                entryTypesManager,
                taskExecutor,
                journalAbbreviationRepository,
                luceneManager,
                searchQueryProperty
        );
    }
}
