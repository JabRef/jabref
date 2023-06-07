package org.jabref.gui.edit;

import javafx.scene.control.TextInputControl;
import javafx.scene.web.WebView;

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
            LOGGER.debug("focusOwner: {}; Action: {}", focusOwner, action.getText());
            if (focusOwner instanceof TextInputControl textInput) {
                // Focus is on text field -> copy/paste/cut selected text
                // DELETE_ENTRY in text field should do forward delete
                switch (action) {
                    case SELECT_ALL -> textInput.selectAll();
                    case COPY -> textInput.copy();
                    case CUT -> textInput.cut();
                    case PASTE -> textInput.paste();
                    case DELETE -> textInput.clear();
                    case DELETE_ENTRY -> textInput.deleteNextChar();
                    case UNDO -> textInput.undo();
                    case REDO -> textInput.redo();
                    default -> {
                        String message = "Only cut/copy/paste supported in TextInputControl but got " + action;
                        LOGGER.error(message);
                        throw new IllegalStateException(message);
                    }
                }
            } else if ((focusOwner instanceof CodeArea) || (focusOwner instanceof WebView)) {
                LOGGER.debug("Ignoring request in CodeArea or WebView");
                return;
            } else {
                LOGGER.debug("Else: {}", focusOwner.getClass().getSimpleName());
                // Not sure what is selected -> copy/paste/cut selected entries except for Preview and CodeArea

                switch (action) {
                    case COPY -> frame.getCurrentLibraryTab().copy();
                    case CUT -> frame.getCurrentLibraryTab().cut();
                    case PASTE -> frame.getCurrentLibraryTab().paste();
                    case DELETE_ENTRY -> frame.getCurrentLibraryTab().delete(false);
                    case UNDO -> {
                        if (frame.getUndoManager().canUndo()) {
                            frame.getUndoManager().undo();
                        }
                    }
                    case REDO -> {
                        if (frame.getUndoManager().canRedo()) {
                            frame.getUndoManager().redo();
                        }
                    }
                    default -> LOGGER.debug("Only cut/copy/paste/deleteEntry supported but got: {} and focus owner {}", action, focusOwner);
                }
            }
        });
    }
}
