package org.jabref.gui.fieldeditors;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.JabRefPreferences;

public class OwnerEditorViewModel extends AbstractEditorViewModel {
    private final JabRefPreferences preferences;

    public OwnerEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, JabRefPreferences preferences, FieldCheckers fieldCheckers) {
        super(field, suggestionProvider, fieldCheckers);
        this.preferences = preferences;
    }

    public void setOwner() {
        text.set(preferences.get(JabRefPreferences.DEFAULT_OWNER));
    }
}
