package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.preferences.PreferencesService;

public class OtherFieldsTab extends FieldsEditorTab {

    public static final String NAME = "Other fields";
    private final List<Field> customTabFieldNames;
    private final BibEntryTypesManager entryTypesManager;

    public OtherFieldsTab(BibDatabaseContext databaseContext,
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
        super(false,
                databaseContext,
                suggestionProviders,
                undoManager,
                dialogService,
                preferences,
                stateManager,
                themeManager,
                taskExecutor,
                journalAbbreviationRepository,
                indexingTaskManager);

        this.entryTypesManager = entryTypesManager;
        this.customTabFieldNames = new ArrayList<>();
        preferences.getEntryEditorPreferences().getDefaultEntryEditorTabs().values().forEach(customTabFieldNames::addAll);

        setText(Localization.lang("Other fields"));
        setTooltip(new Tooltip(Localization.lang("Show remaining fields")));
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
    }

    @Override
    protected Set<Field> determineFieldsToShow(BibEntry entry) {
        BibDatabaseMode mode = databaseContext.getMode();
        Optional<BibEntryType> entryType = entryTypesManager.enrich(entry.getType(), mode);
        if (entryType.isPresent()) {
            Set<Field> allKnownFields = entryType.get().getAllFields();
            Set<Field> otherFields = entry.getFields().stream()
                                          .filter(field -> !allKnownFields.contains(field) &&
                                                  !(field.equals(StandardField.COMMENT) || field instanceof UserSpecificCommentField))
                                          .collect(Collectors.toCollection(LinkedHashSet::new));
            otherFields.removeAll(entryType.get().getDeprecatedFields(mode));
            otherFields.removeAll(entryType.get().getOptionalFields().stream().map(BibField::field).collect(Collectors.toSet()));
            otherFields.remove(InternalField.KEY_FIELD);
            customTabFieldNames.forEach(otherFields::remove);
            return otherFields;
        } else {
            // Entry type unknown -> treat all fields as required
            return Collections.emptySet();
        }
    }
}
