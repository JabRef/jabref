package org.jabref.gui.entryeditor;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.fieldeditors.FieldEditorFX;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.search.indexing.IndexingTaskManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.preferences.PreferencesService;

public class CommentsTab extends FieldsEditorTab {
    public static final String NAME = "Comments";

    private final String defaultOwner;
    public CommentsTab(PreferencesService preferences,
                       BibDatabaseContext databaseContext,
                       SuggestionProviders suggestionProviders,
                       UndoManager undoManager,
                       DialogService dialogService,
                       StateManager stateManager,
                       ThemeManager themeManager,
                       IndexingTaskManager indexingTaskManager,
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
        this.defaultOwner = preferences.getOwnerPreferences().getDefaultOwner();
        setText(Localization.lang("Comments"));
        setGraphic(IconTheme.JabRefIcons.COMMENT.getGraphicNode());
    }

    @Override
    protected Set<Field> determineFieldsToShow(BibEntry entry) {
        UserSpecificCommentField defaultCommentField = new UserSpecificCommentField(defaultOwner);

        // As default: Show BibTeX comment field and the user-specific comment field of the default owner
        Set<Field> comments = new LinkedHashSet<>(Set.of(defaultCommentField, StandardField.COMMENT));

        comments.addAll(entry.getFields().stream()
                             .filter(field -> field instanceof UserSpecificCommentField ||
                                     field.getName().toLowerCase().contains("comment"))
                             .collect(Collectors.toSet()));

        return comments;
    }

    @Override
    protected void setupPanel(BibEntry entry, boolean compressed) {
        super.setupPanel(entry, compressed);

        for (Map.Entry<Field, FieldEditorFX> fieldEditorEntry : editors.entrySet()) {
            Field field = fieldEditorEntry.getKey();
            FieldEditorFX editor = fieldEditorEntry.getValue();

            if (field instanceof UserSpecificCommentField) {
                if (field.getName().contains(defaultOwner)) {
                    editor.getNode().setDisable(false);
                }
            } else {
                editor.getNode().setDisable(!field.getName().equals(StandardField.COMMENT.getName()));
            }
        }
    }
}
