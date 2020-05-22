package org.jabref.gui.metadata;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.UndoablePreambleChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class PreambleEditor extends SimpleCommand {

    private DialogService dialogService;
    private final StateManager stateManager;
    private UndoManager undoManager;

    public PreambleEditor(StateManager stateManager, UndoManager undoManager, DialogService dialogService) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.undoManager = undoManager;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        BibDatabase database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null")).getDatabase();

        TextArea editor = new TextArea();
        editor.setText(database.getPreamble().orElse(""));
        DialogPane pane = new DialogPane();
        pane.setContent(editor);

        Optional<ButtonType> pressedButton = dialogService.showCustomDialogAndWait(Localization.lang("Edit Preamble"), pane, ButtonType.APPLY, ButtonType.CANCEL);

        if (pressedButton.isPresent() && pressedButton.get().equals(ButtonType.APPLY)) {
            String newPreamble = editor.getText();

            // We check if the field has changed, since we don't want to mark the
            // base as changed unless we have a real change.
            if (!database.getPreamble().orElse("").equals(newPreamble)) {
                undoManager.addEdit(new UndoablePreambleChange(database, database.getPreamble().orElse(null), newPreamble));
                database.setPreamble(newPreamble);
            }
        }
    }
}
