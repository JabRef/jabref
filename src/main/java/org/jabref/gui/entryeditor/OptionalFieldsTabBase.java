package org.jabref.gui.entryeditor;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.search.IndexingTaskManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

public class OptionalFieldsTabBase extends FieldsEditorTab {
    private final BibEntryTypesManager entryTypesManager;
    private final boolean isImportantOptionalFields;

    public OptionalFieldsTabBase(String title,
                                 boolean isImportantOptionalFields,
                                 BibDatabaseContext databaseContext,
                                 SuggestionProviders suggestionProviders,
                                 UndoManager undoManager,
                                 UndoAction undoAction,
                                 RedoAction redoAction,
                                 DialogService dialogService,
                                 PreferencesService preferences,
                                 StateManager stateManager,
                                 ThemeManager themeManager,
                                 IndexingTaskManager indexingTaskManager,
                                 BibEntryTypesManager entryTypesManager,
                                 TaskExecutor taskExecutor,
                                 JournalAbbreviationRepository journalAbbreviationRepository) {
        super(true,
                databaseContext,
                suggestionProviders,
                undoManager,
                undoAction,
                redoAction,
                dialogService,
                preferences,
                stateManager,
                themeManager,
                taskExecutor,
                journalAbbreviationRepository,
                indexingTaskManager);
        this.entryTypesManager = entryTypesManager;
        this.isImportantOptionalFields = isImportantOptionalFields;
        setText(title);
        setTooltip(new Tooltip(Localization.lang("Show optional fields")));
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
    }

    @Override
    protected SequencedSet<Field> determineFieldsToShow(BibEntry entry) {
        BibDatabaseMode mode = databaseContext.getMode();
        Optional<BibEntryType> entryType = entryTypesManager.enrich(entry.getType(), mode);
        if (entryType.isPresent()) {
            if (isImportantOptionalFields) {
                return entryType.get().getImportantOptionalFields();
            } else {
                return entryType.get().getDetailOptionalNotDeprecatedFields(mode);
            }
        } else {
            // Entry type unknown -> treat all fields as required (thus no optional fields)
            return new LinkedHashSet<>();
        }
    }
}
