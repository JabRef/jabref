package org.jabref.gui.metadata;

import java.util.Optional;

import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.UndoablePreambleChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;

public class PreambleEditor extends SimpleCommand {

    private final TextArea editor = new TextArea();

    private final JabRefFrame frame;

    public PreambleEditor(JabRefFrame frame) {
        this.frame = frame;
    }

    @Override
    public void execute() {
        BasePanel panel = frame.getCurrentBasePanel();
        BibDatabase database = frame.getCurrentBasePanel().getDatabase();

        DialogPane pane = new DialogPane();

        editor.setText(frame.getCurrentBasePanel()
                            .getDatabase()
                            .getPreamble()
                            .orElse(""));
        pane.setContent(editor);

        Optional<ButtonType> pressedButton = frame.getDialogService().showCustomDialogAndWait(Localization.lang("Edit Preamble"), pane, ButtonType.APPLY, ButtonType.CANCEL);

        if (pressedButton.isPresent() && pressedButton.get().equals(ButtonType.APPLY)) {
            String newPreamble = editor.getText();

            // We check if the field has changed, since we don't want to mark the
            // base as changed unless we have a real change.
            if (!database.getPreamble().orElse("").equals(newPreamble)) {

                panel.getUndoManager().addEdit(
                        new UndoablePreambleChange(database, database.getPreamble().orElse(null), newPreamble));
                database.setPreamble(newPreamble);

                panel.markBaseChanged();
            }
        }

    }

}
