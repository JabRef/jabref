package org.jabref.gui.edit;

import javafx.scene.control.TextInputControl;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;

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
            LOGGER.debug("EditAction - focusOwner: {}; Action: {}", focusOwner.toString(), action.getText());
            if (focusOwner instanceof TextInputControl) {
                // Focus is on text field -> copy/paste/cut selected text
                TextInputControl textInput = (TextInputControl) focusOwner;
                switch (action) {
                    case COPY:
                        textInput.copy();
                        break;
                    case CUT:
                        textInput.cut();
                        break;
                    case PASTE:
                        textInput.paste();
                        break;
                    case DELETE_ENTRY:
                        // DELETE_ENTRY in text field should do forward delete
                        textInput.deleteNextChar();
                        break;
                    default:
                        throw new IllegalStateException("Only cut/copy/paste supported in TextInputControl but got " + action);
                }

            } else {

                LOGGER.debug("EditAction - Else: {}", frame.getCurrentBasePanel().getTabTitle());
                // Not sure what is selected -> copy/paste/cut selected entries

                // ToDo: Should be handled by BibDatabaseContext instead of BasePanel
                switch (action) {
                    case COPY:
                        frame.getCurrentBasePanel().copy();
                        break;
                    case CUT:
                        frame.getCurrentBasePanel().cut();
                        break;
                    case PASTE:
                        frame.getCurrentBasePanel().paste();
                        break;
                    case DELETE_ENTRY:
                        frame.getCurrentBasePanel().delete(false);
                        break;
                    default:
                        throw new IllegalStateException("Only cut/copy/paste supported but got " + action);
                }
            }
        });
    }
}
