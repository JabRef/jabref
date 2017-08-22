package org.jabref.gui.fieldeditors;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;

public class KeywordsEditor extends SimpleEditor implements FieldEditorFX {

    public KeywordsEditor(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers) {
        super (fieldName, suggestionProvider, fieldCheckers);
    }

    @Override
    public int getWeight() {
        return 2;
    }
}
