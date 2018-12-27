package org.jabref.gui.fieldeditors;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.preferences.JabRefPreferences;

public class MultilineEditor extends SimpleEditor implements FieldEditorFX {

    public MultilineEditor(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, JabRefPreferences preferences) {
        super (fieldName, suggestionProvider, fieldCheckers, preferences);
    }

    @Override
    public double getWeight() {
        return 4;
    }
}
