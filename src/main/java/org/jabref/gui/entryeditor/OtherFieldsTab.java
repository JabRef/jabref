package org.jabref.gui.entryeditor;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;

public class OtherFieldsTab extends FieldsEditorTab {

    private final List<Field> customTabFieldNames;

    public OtherFieldsTab(BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders, UndoManager undoManager, List<Field> customTabFieldNames, DialogService dialogService) {
        super(false, databaseContext, suggestionProviders, undoManager, dialogService);

        setText(Localization.lang("Other fields"));
        setTooltip(new Tooltip(Localization.lang("Show remaining fields")));
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
        this.customTabFieldNames = customTabFieldNames;
    }

    @Override
    protected Collection<Field> determineFieldsToShow(BibEntry entry, BibEntryType entryType) {
        Set<Field> allKnownFields = new HashSet<>(entryType.getAllFields());
        List<Field> otherFields = entry.getFieldNames().stream().filter(field -> !allKnownFields.contains(field)).collect(Collectors.toList());

        otherFields.removeAll(entryType.getDeprecatedFields());
        otherFields.removeAll(entryType.getOptionalFields());
        otherFields.remove(InternalField.KEY_FIELD);
        otherFields.removeAll(customTabFieldNames);
        return otherFields;
    }
}
