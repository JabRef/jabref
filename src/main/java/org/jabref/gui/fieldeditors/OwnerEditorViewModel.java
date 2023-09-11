package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

public class OwnerEditorViewModel extends AbstractEditorViewModel {
    private final PreferencesService preferences;

    public OwnerEditorViewModel(Field field,
                                SuggestionProvider<?> suggestionProvider,
                                PreferencesService preferences,
                                FieldCheckers fieldCheckers,
                                UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager);
        this.preferences = preferences;
    }

    public void setOwner() {
        text.set(preferences.getOwnerPreferences().getDefaultOwner());
    }
}
