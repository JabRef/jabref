package org.jabref.gui.genfields;

import java.util.List;
import java.util.Map;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;
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

        //TODO: add method getEntryEditorTabList to PreferencesService interface
        for (Map.Entry<String, List<String>> tab : Globals.prefs.getEntryEditorTabList().entrySet()) {
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

            //TODO: create a method in preferences which wraps the get enfore legal keys and add it to preferneces service as well
            String testString = BibtexKeyGenerator.cleanKey(parts[1], Globals.prefs.getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));

            //Unfinished
        }
    }

    public void resetFields() {

    }
}
