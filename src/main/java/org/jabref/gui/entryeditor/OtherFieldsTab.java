package org.jabref.gui.entryeditor;

import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.control.Tooltip;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;

public class OtherFieldsTab extends FieldsEditorTab {

    public OtherFieldsTab(JabRefFrame frame, BasePanel basePanel, EntryType entryType, EntryEditor parent, BibEntry entry) {
        super(frame, basePanel, getOtherFields(entryType, entry), parent, false, false, entry);

        setText(Localization.lang("Other fields"));
        setTooltip(new Tooltip(Localization.lang("Show remaining fields")));
        setGraphic(IconTheme.JabRefIcon.OPTIONAL.getGraphicNode());
    }

    private static List<String> getOtherFields(EntryType entryType, BibEntry entry) {
        List<String> allKnownFields = entryType.getAllFields().stream().map(String::toLowerCase)
                .collect(Collectors.toList());
        List<String> otherFields = entry.getFieldNames().stream().map(String::toLowerCase)
                .filter(field -> !allKnownFields.contains(field)).collect(Collectors.toList());

        otherFields.removeAll(entryType.getDeprecatedFields());
        otherFields.remove(BibEntry.KEY_FIELD);
        otherFields.removeAll(Globals.prefs.getCustomTabFieldNames());
        return otherFields;
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
}
