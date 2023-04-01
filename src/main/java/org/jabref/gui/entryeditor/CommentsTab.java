package org.jabref.gui.entryeditor;

import java.util.HashSet;
import java.util.Set;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.search.indexing.IndexingTaskManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.preferences.PreferencesService;

public class CommentsTab extends FieldsEditorTab {

    private final BibEntryTypesManager entryTypesManager;

    private final String name;
    public CommentsTab(String name,
                       BibDatabaseContext databaseContext,
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
                false,
                databaseContext,
                suggestionProviders,
                undoManager,
                dialogService,
                preferences,
                stateManager,
                themeManager,
                taskExecutor,
                journalAbbreviationRepository,
                indexingTaskManager
        );
        this.entryTypesManager = entryTypesManager;
        this.name = name;
        setText(Localization.lang("All comments"));
        setTooltip(new Tooltip(Localization.lang("Display all comments")));
        setGraphic(IconTheme.JabRefIcons.REQUIRED.getGraphicNode());
    }

    protected Set<Field> determineFieldsToShow(BibEntry entry) {
        Set<Field> fieldsToShow = new HashSet<>();
        fieldsToShow.add(new UserSpecificCommentField(name));
        Set<Field> allFields = entry.getFields();
        for (Field field : allFields) {
            if (field.getProperties().contains(FieldProperty.COMMENT)) {
                fieldsToShow.add(field);
            }
        }
        return fieldsToShow;
    }
}
