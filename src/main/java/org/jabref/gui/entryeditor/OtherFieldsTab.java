package org.jabref.gui.entryeditor;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.preferences.JabRefPreferences;

public class OtherFieldsTab extends FieldsEditorTab {

    private final List<Field> customTabFieldNames;
    private final BibEntryTypesManager entryTypesManager;

    public OtherFieldsTab(BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders, UndoManager undoManager, List<Field> customTabFieldNames, DialogService dialogService, JabRefPreferences preferences, BibEntryTypesManager entryTypesManager, ExternalFileTypes externalFileTypes, TaskExecutor taskExecutor, JournalAbbreviationLoader journalAbbreviationLoader) {
        super(false, databaseContext, suggestionProviders, undoManager, dialogService, preferences, externalFileTypes, taskExecutor, journalAbbreviationLoader);
        this.entryTypesManager = entryTypesManager;
        this.customTabFieldNames = customTabFieldNames;

        setText(Localization.lang("Other fields"));
        setTooltip(new Tooltip(Localization.lang("Show remaining fields")));
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
    }

    @Override
    protected SortedSet<Field> determineFieldsToShow(BibEntry entry) {
        Optional<BibEntryType> entryType = entryTypesManager.enrich(entry.getType(), databaseContext.getMode());
        if (entryType.isPresent()) {
            Set<Field> allKnownFields = entryType.get().getAllFields().stream().map(BibField::getField).collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Field::getName))));
            SortedSet<Field> otherFields = entry.getFields().stream().filter(field -> !allKnownFields.contains(field)).collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Field::getName))));

            otherFields.removeAll(entryType.get().getDeprecatedFields());
            otherFields.removeAll(entryType.get().getOptionalFields().stream().map(BibField::getField).collect(Collectors.toSet()));
            otherFields.remove(InternalField.KEY_FIELD);
            otherFields.removeAll(customTabFieldNames);
            return otherFields;
        } else {
            // Entry type unknown -> treat all fields as required
            return Collections.emptySortedSet();
        }
    }
}
