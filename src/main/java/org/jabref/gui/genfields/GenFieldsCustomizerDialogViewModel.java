package org.jabref.gui.genfields;

import java.util.List;
import java.util.Map;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

public class GenFieldsCustomizerDialogViewModel {

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final StringProperty fieldsText = new SimpleStringProperty("");

    public GenFieldsCustomizerDialogViewModel(DialogService dialogService, PreferencesService preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        setInitialFieldsText();
    }

    private void setInitialFieldsText() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, List<String>> tab : preferences.getEntryEditorTabList().entrySet()) {
            sb.append(tab.getKey());
            sb.append(':');
            sb.append(String.join(";", tab.getValue()));
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

            //Unfinished
        }
    }

    public void resetFields() {

        StringBuilder sb = new StringBuilder();
        String name;
        String fields;
        int i = 0;
        //threre may be a better var name
        //You can make getgetCustomTabFieldNames depend on getTabNamesAndFields
        //but that's refactoring and needs to be tested where it appears
        //YOu can have a while loop in the preferences method - use a 'for' here
        //Make the Map a HashMap in preferences
        Map<String,String> customTabNamesFields = preferences.getCustomTabsNamesAndFields();
        for (Map.Entry<String,String>entry : customTabNamesFields.entrySet()) {
            sb.append(entry.getKey());
            sb.append(':');
            sb.append(entry.getValue());
            sb.append('\n');
        fieldsText.set(sb.toString());
        }
    }
}
