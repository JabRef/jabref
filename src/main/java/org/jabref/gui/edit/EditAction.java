package org.jabref.gui.edit;

import javafx.scene.control.TextInputControl;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;

import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for handling general actions; cut, copy and paste. The focused component is kept track of by
 * Globals.focusListener, and we call the action stored under the relevant name in its action map.
 */
public class EditAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditAction.class);

    private final JabRefFrame frame;
    private final StandardActions action;
    private final StateManager stateManager;

    public EditAction(StandardActions action, JabRefFrame frame, StateManager stateManager) {
        this.action = action;
        this.frame = frame;
        this.stateManager = stateManager;

        if (action == StandardActions.PASTE) {
            this.executable.bind(ActionHelper.needsDatabase(stateManager));
        } else {
            this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
        }
    }

    @Override
    public String toString() {
        return this.action.toString();
    }

    @Override
    public void execute() {
        stateManager.getFocusOwner().ifPresent(focusOwner -> {
            LOGGER.debug("focusOwner: {}; Action: {}", focusOwner.toString(), action.getText());
            if (focusOwner instanceof TextInputControl) {
                // Focus is on text field -> copy/paste/cut selected text
                TextInputControl textInput = (TextInputControl) focusOwner;
                // DELETE_ENTRY in text field should do forward delete
                switch (action) {
                    case COPY -> textInput.copy();
                    case CUT -> textInput.cut();
                    case PASTE -> textInput.paste();
                    case DELETE_ENTRY -> textInput.deleteNextChar();
                    default -> throw new IllegalStateException("Only cut/copy/paste supported in TextInputControl but got " + action);
                }

            } else if (!(focusOwner instanceof CodeArea)) {

                LOGGER.debug("Else: {}", focusOwner.getClass().getSimpleName());
                // Not sure what is selected -> copy/paste/cut selected entries

                // ToDo: Should be handled by BibDatabaseContext instead of LibraryTab
                switch (action) {
                    case COPY -> frame.getCurrentLibraryTab().copy();
                    case CUT -> frame.getCurrentLibraryTab().cut();
                    case PASTE -> frame.getCurrentLibraryTab().paste();
                    case DELETE_ENTRY -> frame.getCurrentLibraryTab().delete(false);
                    default -> throw new IllegalStateException("Only cut/copy/paste supported but got " + action);
                }
            }
        });
    }
}
