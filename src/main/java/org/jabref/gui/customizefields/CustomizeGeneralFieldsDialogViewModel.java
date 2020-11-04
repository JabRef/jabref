package org.jabref.gui.customizefields;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.preferences.PreferencesService;

public class CustomizeGeneralFieldsDialogViewModel {

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final StringProperty fieldsText = new SimpleStringProperty("");

    public CustomizeGeneralFieldsDialogViewModel(DialogService dialogService, PreferencesService preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;

        // Using stored custom values or, if they do not exist, default values
        setFieldsText(preferences.getEntryEditorTabList());
    }

    private void setFieldsText(Map<String, Set<Field>> tabNamesAndFields) {
        StringBuilder sb = new StringBuilder();

        // Fill with customized vars
        for (Map.Entry<String, Set<Field>> tab : tabNamesAndFields.entrySet()) {
            sb.append(tab.getKey());
            sb.append(':');
            sb.append(FieldFactory.serializeFieldsList(tab.getValue()));
            sb.append('\n');
        }
        fieldsText.set(sb.toString());
    }

    public StringProperty fieldsTextProperty() {
        return fieldsText;
    }

    public void saveFields() {
        Map<String, Set<Field>> customTabsMap = new LinkedHashMap<>();
        String[] lines = fieldsText.get().split("\n");

        for (String line : lines) {
            String[] parts = line.split(":");
            if (parts.length != 2) {
                dialogService.showInformationDialogAndWait(
                        Localization.lang("Error"),
                        Localization.lang("Each line must be of the following form: 'tab:field1;field2;...;fieldN'."));
                return;
            }

            // Use literal string of unwanted characters specified below as opposed to exporting characters
            // from preferences because the list of allowable characters in this particular differs
            // i.e. ';' character is allowed in this window, but it's on the list of unwanted chars in preferences
            String unwantedChars = "#{}()~,^&-\"'`ʹ\\";
            String testString = CitationKeyGenerator.cleanKey(parts[1], unwantedChars);
            if (!testString.equals(parts[1])) {
                dialogService.showInformationDialogAndWait(
                        Localization.lang("Error"),
                        Localization.lang("Field names are not allowed to contain white spaces or certain characters (%0).",
                                "# { } ( ) ~ , ^ & - \" ' ` ʹ \\"));
                return;
            }

            customTabsMap.put(parts[0], FieldFactory.parseFieldList(parts[1]));
        }

        preferences.storeEntryEditorTabList(customTabsMap);
    }

    public void resetFields() {
        // Using default values
        setFieldsText(preferences.getDefaultTabNamesAndFields());
    }
}
