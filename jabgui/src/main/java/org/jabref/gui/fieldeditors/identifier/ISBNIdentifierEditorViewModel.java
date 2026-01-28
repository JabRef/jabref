package org.jabref.gui.fieldeditors.identifier;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ISBN;

public class ISBNIdentifierEditorViewModel extends BaseIdentifierEditorViewModel<ISBN> {
    public ISBNIdentifierEditorViewModel(SuggestionProvider<?> suggestionProvider,
                                         FieldCheckers fieldCheckers,
                                         DialogService dialogService,
                                         TaskExecutor taskExecutor,
                                         GuiPreferences preferences,
                                         UndoManager undoManager,
                                         StateManager stateManager) {
        super(StandardField.ISBN, suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferences, undoManager, stateManager);
        configure(true, false, false);
    }
}
