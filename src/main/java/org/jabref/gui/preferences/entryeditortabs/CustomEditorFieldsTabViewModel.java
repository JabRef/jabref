package org.jabref.gui.preferences.entryeditortabs;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreferencesService;

public class CustomEditorFieldsTabViewModel implements PreferenceTabViewModel {

    private final StringProperty fieldsProperty = new SimpleStringProperty();

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final EntryEditorPreferences entryEditorPreferences;

    public CustomEditorFieldsTabViewModel(DialogService dialogService, PreferencesService preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.entryEditorPreferences = preferences.getEntryEditorPreferences();
    }

    @Override
    public void setValues() {
        setFields(entryEditorPreferences.getEntryEditorTabList());
    }

    public void resetToDefaults() {
        setFields(preferences.getDefaultTabNamesAndFields());
    }

    private void setFields(Map<String, Set<Field>> tabNamesAndFields) {
        StringBuilder sb = new StringBuilder();

        // Fill with customized vars
        int i = 0;
        for (Map.Entry<String, Set<Field>> tab : tabNamesAndFields.entrySet()) {
            String key = tab.getKey();
            if (key.equals(preferences.getDefaults().get(JabRefPreferences.CUSTOM_TAB_NAME + "_def" + i))) {
                key = Localization.lang(key);
            }
            Set<Field> fields = tab.getValue();
            if (fields.equals(FieldFactory.parseFieldList((String) preferences.getDefaults().get(JabRefPreferences.CUSTOM_TAB_FIELDS + "_def" + i)))) {
                fields = fields.stream().map(Field::getName).map(Localization::lang).map(FieldFactory::parseField).collect(Collectors.toSet());
            }
            i++;
            sb.append(key);
            sb.append(':');
            sb.append(FieldFactory.serializeFieldsList(fields));
            sb.append('\n');
        }
        fieldsProperty.set(sb.toString());
    }

    @Override
    public void storeSettings() {
        Map<String, Set<Field>> customTabsMap = new LinkedHashMap<>();
        String[] lines = fieldsProperty.get().split("\n");

        int i = 0;
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
            String key = parts[0];
            String defaultKey = (String) preferences.getDefaults().get(JabRefPreferences.CUSTOM_TAB_NAME + "_def" + i);
            if (key.equals(Localization.lang(defaultKey))) {
                key = defaultKey;
            }
            Set<Field> fields = FieldFactory.parseFieldList(parts[1]);
            Set<Field> defaultFields = FieldFactory.parseFieldList((String) preferences.getDefaults().get(JabRefPreferences.CUSTOM_TAB_FIELDS + "_def" + i));
            Set<Field> defaultLocalizedFields = defaultFields.stream().map(Field::getName).map(Localization::lang).map(fieldName -> FieldFactory.parseField(fieldName)).collect(Collectors.toSet());
            if (fields.equals(defaultLocalizedFields)) {
                fields = defaultFields;
            }
            customTabsMap.put(key, fields);
            i++;
        }

        entryEditorPreferences.setEntryEditorTabList(customTabsMap);
    }

    /**
     * This method is not meant to be used. It only makes sure the Localization keys used as Defaults in CUSTOM_TAB_NAME
     * are used once and therefore do not show up in LocalizationConsistencyTest
     */
    private void makeDefaultFieldNamesNotObsoleteLocalizationFields() {
        Localization.lang("Abstract");
        Localization.lang("Comments");
        Localization.lang("References");
    }

    public StringProperty fieldsProperty() {
        return fieldsProperty;
    }
}
