package org.jabref.gui.entryeditor;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.search.indexing.IndexingTaskManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

public class OptionalFieldsTabBase extends FieldsEditorTab {
    private final BibEntryTypesManager entryTypesManager;
    private final boolean isPrimaryOptionalFields;

    public OptionalFieldsTabBase(String title,
                                 boolean isPrimaryOptionalFields,
                                 BibDatabaseContext databaseContext,
                                 SuggestionProviders suggestionProviders,
                                 UndoManager undoManager,
                                 DialogService dialogService,
                                 PreferencesService preferences,
                                 StateManager stateManager,
                                 IndexingTaskManager indexingTaskManager,
                                 BibEntryTypesManager entryTypesManager,
                                 ExternalFileTypes externalFileTypes,
                                 TaskExecutor taskExecutor,
                                 JournalAbbreviationRepository journalAbbreviationRepository) {
        super(true, databaseContext, suggestionProviders, undoManager, dialogService, preferences, stateManager, externalFileTypes, taskExecutor, journalAbbreviationRepository, indexingTaskManager);
        this.entryTypesManager = entryTypesManager;
        this.isPrimaryOptionalFields = isPrimaryOptionalFields;
        setText(title);
        setTooltip(new Tooltip(Localization.lang("Show optional fields")));
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
    }

    @Override
    protected Set<Field> determineFieldsToShow(BibEntry entry) {
        Optional<BibEntryType> entryType = entryTypesManager.enrich(entry.getType(), databaseContext.getMode());
        if (entryType.isPresent()) {
            if (isPrimaryOptionalFields) {
                return entryType.get().getPrimaryOptionalFields();
            } else {
                return entryType.get().getSecondaryOptionalNotDeprecatedFields();
            }
        } else {
            // Entry type unknown -> treat all fields as required
            return Collections.emptySet();
        }
    }
}
