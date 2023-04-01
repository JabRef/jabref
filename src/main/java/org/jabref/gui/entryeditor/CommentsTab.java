package org.jabref.gui.entryeditor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
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
        BibDatabaseMode mode = databaseContext.getMode();
        Optional<BibEntryType> entryType = entryTypesManager.enrich(entry.getType(), mode);

        if (entryType.isPresent()) {
            UserSpecificCommentField defaultCommentField = new UserSpecificCommentField(name);
            Set<Field> comments = new LinkedHashSet<>();
            comments.add(defaultCommentField);

            // Add other comment fields from the entry
            comments.addAll(entry.getFields().stream()
                                    .filter(field -> field.equals(StandardField.COMMENT) || field instanceof UserSpecificCommentField)
                                    .collect(Collectors.toSet()));

            return comments;
        } else {
            // Entry type unknown -> treat all fields as required
            return Collections.emptySet();
        }
    }
}
