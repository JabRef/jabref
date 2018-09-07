package org.jabref.gui.genfields;

import java.util.List;
import java.util.Map;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.Globals;
import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.preferences.PreferencesService;

public class GenFieldsCustomizerDialogViewModel extends AbstractViewModel {

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final ObjectProperty<String> initialFieldsText = new SimpleObjectProperty<>();

    public GenFieldsCustomizerDialogViewModel(DialogService dialogService, PreferencesService preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        setInitialFieldsText();
    }

    private void setInitialFieldsText() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, List<String>> tab : Globals.prefs.getEntryEditorTabList().entrySet()) {
            sb.append(tab.getKey());
            sb.append(':');
            sb.append(String.join(";", tab.getValue()));
            sb.append('\n');
        }

        initialFieldsText.set(sb.toString());
    }

}