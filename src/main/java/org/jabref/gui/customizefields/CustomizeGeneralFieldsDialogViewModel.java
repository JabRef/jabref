package org.jabref.gui.customizefields;

import java.util.Map;
import java.util.Set;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreferencesService;

public class CustomizeGeneralFieldsDialogViewModel {

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final StringProperty fieldsText = new SimpleStringProperty("");

    public CustomizeGeneralFieldsDialogViewModel(DialogService dialogService, PreferencesService preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        setInitialFieldsText();
    }

    private void setInitialFieldsText() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Set<Field>> tab : preferences.getEntryEditorTabList().entrySet()) {
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
        String[] lines = fieldsText.get().split("\n");
        int i = 0;
        for (; i < lines.length; i++) {
            String[] parts = lines[i].split(":");
            if (parts.length != 2) {
                // Report error and exit.
                String field = Localization.lang("field");
                String title = Localization.lang("Error");
                String content = Localization.lang("Each line must be of the following form") + " '" +
                                 Localization.lang("Tabname") + ':' + field + "1;" + field + "2;...;" + field + "N'";
                dialogService.showInformationDialogAndWait(title, content);
                return;
            }

            String testString = BibtexKeyGenerator.cleanKey(parts[1], preferences.getEnforceLegalKeys());
            if (!testString.equals(parts[1]) || (parts[1].indexOf('&') >= 0)) {
                String title = Localization.lang("Error");
                String content = Localization.lang("Field names are not allowed to contain white space or the following "
                                                   + "characters")
                                 + ": # { } ( ) ~ , ^ & - \" ' ` ʹ \\";
                dialogService.showInformationDialogAndWait(title, content);
                return;
            }
            preferences.setCustomTabsNameAndFields(parts[0], parts[1], i);

        }
        preferences.purgeSeries(JabRefPreferences.CUSTOM_TAB_NAME, i);
        preferences.purgeSeries(JabRefPreferences.CUSTOM_TAB_FIELDS, i);
        preferences.updateEntryEditorTabList();
    }

    public void resetFields() {

        StringBuilder sb = new StringBuilder();
        Map<String,String> customTabNamesFields = preferences.getCustomTabsNamesAndFields();
        for (Map.Entry<String,String>entry : customTabNamesFields.entrySet()) {
            sb.append(entry.getKey());
            sb.append(':');
            sb.append(entry.getValue());
            sb.append('\n');
        }
        fieldsText.set(sb.toString());

    }
}
