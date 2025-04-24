package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.AppendWordsStrategy;
import org.jabref.gui.autocompleter.AutoCompletionStrategy;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.Field;

public class SimpleEditorViewModel extends AbstractEditorViewModel {

    public SimpleEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager);
    }

    public AutoCompletionStrategy getAutoCompletionStrategy() {
        return new AppendWordsStrategy();
    }
}
