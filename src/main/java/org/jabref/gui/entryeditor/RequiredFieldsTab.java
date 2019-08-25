package org.jabref.gui.entryeditor;

import java.util.Comparator;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Tooltip;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.OrFields;

public class RequiredFieldsTab extends FieldsEditorTab {
    public RequiredFieldsTab(BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders, UndoManager undoManager, DialogService dialogService) {
        super(false, databaseContext, suggestionProviders, undoManager, dialogService);

        setText(Localization.lang("Required fields"));
        setTooltip(new Tooltip(Localization.lang("Show required fields")));
        setGraphic(IconTheme.JabRefIcons.REQUIRED.getGraphicNode());
    }

    @Override
    protected SortedSet<Field> determineFieldsToShow(BibEntry entry) {
        Optional<BibEntryType> entryType = Globals.entryTypesManager.enrich(entry.getType(), databaseContext.getMode());
        SortedSet<Field> fields = new TreeSet<>(Comparator.comparing(Field::getName));
        if (entryType.isPresent()) {
            fields.addAll(entryType.get().getRequiredFields().stream().map(OrFields::getPrimary).collect(Collectors.toSet()));
            // Add the edit field for Bibtex-key.
            fields.add(InternalField.KEY_FIELD);
        } else {
            // Entry type unknown -> treat all fields as required
            fields.addAll(entry.getFields());
        }
        return fields;
    }
}
