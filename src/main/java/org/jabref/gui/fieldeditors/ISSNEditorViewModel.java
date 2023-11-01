package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Button;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.Field;

public class ISSNEditorViewModel extends AbstractEditorViewModel {
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;

    public ISSNEditorViewModel(
            Field field,
            SuggestionProvider<?> suggestionProvider,
            FieldCheckers fieldCheckers,
            TaskExecutor taskExecutor,
            DialogService dialogService,
            UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager);
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
    }

    public void showJournalInfo(Button journalInfoButton) {
        PopOverUtil.showJournalInfo(journalInfoButton, entry, dialogService, taskExecutor);
    }
}
