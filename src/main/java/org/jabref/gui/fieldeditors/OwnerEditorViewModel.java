package org.jabref.gui.fieldeditors;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.JabRefPreferences;

public class OwnerEditorViewModel extends AbstractEditorViewModel {
    private final JabRefPreferences preferences;

    public OwnerEditorViewModel(Field field, AutoCompleteSuggestionProvider<?> suggestionProvider, JabRefPreferences preferences, FieldCheckers fieldCheckers) {
        super(field, suggestionProvider, fieldCheckers);
        this.preferences = preferences;
    }

    public void setOwner() {
        text.set(preferences.get(JabRefPreferences.DEFAULT_OWNER));
    }
}
