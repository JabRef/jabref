package org.jabref.gui.entryeditor;

import java.util.LinkedHashSet;
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
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.OrFields;
import org.jabref.preferences.PreferencesService;

public class RequiredFieldsTab extends FieldsEditorTab {

    private final BibEntryTypesManager entryTypesManager;

    public RequiredFieldsTab(BibDatabaseContext databaseContext,
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
        super(false, databaseContext, suggestionProviders, undoManager, dialogService,
                preferences, stateManager, externalFileTypes, taskExecutor, journalAbbreviationRepository, indexingTaskManager);
        this.entryTypesManager = entryTypesManager;

        setText(Localization.lang("Required fields"));
        setTooltip(new Tooltip(Localization.lang("Show required fields")));
        setGraphic(IconTheme.JabRefIcons.REQUIRED.getGraphicNode());
    }

    @Override
    protected Set<Field> determineFieldsToShow(BibEntry entry) {
        Optional<BibEntryType> entryType = entryTypesManager.enrich(entry.getType(), databaseContext.getMode());
        Set<Field> fields = new LinkedHashSet<>();
        if (entryType.isPresent()) {
            for (OrFields orFields : entryType.get().getRequiredFields()) {
                fields.addAll(orFields);
            }
            // Add the edit field for Bibtex-key.
            fields.add(InternalField.KEY_FIELD);
        } else {
            // Entry type unknown -> treat all fields as required
            fields.addAll(entry.getFields());
        }
        return fields;
    }
}
