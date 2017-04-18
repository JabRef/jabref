package org.jabref.gui.fieldeditors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.preferences.JabRefPreferences;

public class OwnerEditorViewModel extends AbstractViewModel {
    private final JabRefPreferences preferences;
    private StringProperty text = new SimpleStringProperty("");

    public OwnerEditorViewModel(JabRefPreferences preferences) {
        this.preferences = preferences;
    }

    public StringProperty textProperty() {
        return text;
    }

    public void setOwner() {
        text.set(preferences.get(JabRefPreferences.DEFAULT_OWNER));
    }
}
