package org.jabref.gui.fieldeditors;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.preferences.JabRefPreferences;

public class OwnerEditorViewModel extends AbstractEditorViewModel {
    private final JabRefPreferences preferences;

    public OwnerEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, JabRefPreferences preferences) {
        super(fieldName, suggestionProvider);
        this.preferences = preferences;
    }

    public void setOwner() {
        text.set(preferences.get(JabRefPreferences.DEFAULT_OWNER));
    }
}
