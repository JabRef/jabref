package org.jabref.gui.entryeditor;

import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.control.Tooltip;

import org.jabref.Globals;
import org.jabref.gui.IconTheme;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;

public class OtherFieldsTab extends FieldsEditorTab {

    public OtherFieldsTab(BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders) {
        super(false, databaseContext, suggestionProviders);

        setText(Localization.lang("Other fields"));
        setTooltip(new Tooltip(Localization.lang("Show remaining fields")));
        setGraphic(IconTheme.JabRefIcon.OPTIONAL.getGraphicNode());
    }

    public static boolean isOtherField(EntryType entryType, String fieldToCheck) {
        List<String> allKnownFields = entryType.getAllFields().stream().map(String::toLowerCase)
                .collect(Collectors.toList());
        if (allKnownFields.contains(fieldToCheck) ||
                entryType.getDeprecatedFields().contains(fieldToCheck) ||
                BibEntry.KEY_FIELD.equals(fieldToCheck) ||
                Globals.prefs.getCustomTabFieldNames().contains(fieldToCheck)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected List<String> determineFieldsToShow(BibEntry entry, EntryType entryType) {
        List<String> allKnownFields = entryType.getAllFields().stream().map(String::toLowerCase)
                .collect(Collectors.toList());
        List<String> otherFields = entry.getFieldNames().stream().map(String::toLowerCase)
                .filter(field -> !allKnownFields.contains(field)).collect(Collectors.toList());

        otherFields.removeAll(entryType.getDeprecatedFields());
        otherFields.remove(BibEntry.KEY_FIELD);
        otherFields.removeAll(Globals.prefs.getCustomTabFieldNames());
        return otherFields;
    }
}
