package org.jabref.gui.fieldeditors;

import org.jabref.logic.autocompleter.ContentAutoCompleters;
import org.jabref.preferences.JabRefPreferences;

public class OwnerEditorViewModel extends AbstractEditorViewModel {
    private final JabRefPreferences preferences;

    public OwnerEditorViewModel(String fieldName, ContentAutoCompleters autoCompleter, JabRefPreferences preferences) {
        super(fieldName, autoCompleter);
        this.preferences = preferences;
    }

    public void setOwner() {
        text.set(preferences.get(JabRefPreferences.DEFAULT_OWNER));
    }
}
