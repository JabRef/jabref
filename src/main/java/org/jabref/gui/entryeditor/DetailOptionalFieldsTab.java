package org.jabref.gui.entryeditor;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

public class DetailOptionalFieldsTab extends OptionalFieldsTabBase {

    public static final String NAME = "Optional fields 2";

    public DetailOptionalFieldsTab(BibDatabaseContext databaseContext,
                                   SuggestionProviders suggestionProviders,
                                   UndoManager undoManager,
                                   UndoAction undoAction,
                                   RedoAction redoAction,
                                   GuiPreferences preferences,
                                   BibEntryTypesManager entryTypesManager,
                                   JournalAbbreviationRepository journalAbbreviationRepository,
                                   PreviewPanel previewPanel) {
        super(
                Localization.lang("Optional fields 2"),
                false,
                databaseContext,
                suggestionProviders,
                undoManager,
                undoAction,
                redoAction,
                preferences,
                entryTypesManager,
                journalAbbreviationRepository,
                previewPanel
        );
    }
}
