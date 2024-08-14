package org.jabref.gui.entryeditor;

import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.Set;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.pdf.search.IndexingTaskManager;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

public class UserDefinedFieldsTab extends FieldsEditorTab {
    private final LinkedHashSet<Field> fields;

    public UserDefinedFieldsTab(String name,
                                Set<Field> fields,
                                BibDatabaseContext databaseContext,
                                SuggestionProviders suggestionProviders,
                                UndoManager undoManager,
                                UndoAction undoAction,
                                RedoAction redoAction,
                                DialogService dialogService,
                                PreferencesService preferences,
                                ThemeManager themeManager,
                                IndexingTaskManager indexingTaskManager,
                                TaskExecutor taskExecutor,
                                JournalAbbreviationRepository journalAbbreviationRepository,
                                OptionalObjectProperty<SearchQuery> searchQueryProperty) {
        super(false, databaseContext, suggestionProviders, undoManager, undoAction, redoAction, dialogService, preferences, themeManager, taskExecutor, journalAbbreviationRepository, indexingTaskManager, searchQueryProperty);

        this.fields = new LinkedHashSet<>(fields);

        setText(name);
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
    }

    @Override
    protected SequencedSet<Field> determineFieldsToShow(BibEntry entry) {
        return fields;
    }
}
