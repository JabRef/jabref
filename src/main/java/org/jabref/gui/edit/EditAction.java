package org.jabref.gui.edit;

import java.util.function.Supplier;

import javax.swing.undo.UndoManager;

import javafx.scene.control.TextInputControl;
import javafx.scene.web.WebView;

import org.jabref.gui.LibraryTab;
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

    private final Supplier<LibraryTab> tabSupplier;
    private final StandardActions action;
    private final StateManager stateManager;
    private final UndoManager undoManager;

    public EditAction(StandardActions action, Supplier<LibraryTab> tabSupplier, StateManager stateManager, UndoManager undoManager) {
        this.action = action;
        this.tabSupplier = tabSupplier;
        this.stateManager = stateManager;
        this.undoManager = undoManager;

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
                    case COPY -> tabSupplier.get().copy();
                    case CUT -> tabSupplier.get().cut();
                    case PASTE -> tabSupplier.get().paste();
                    case DELETE_ENTRY -> tabSupplier.get().delete(StandardActions.DELETE_ENTRY);
                    case UNDO -> {
                        if (undoManager.canUndo()) {
                            undoManager.undo();
                        }
                    }
                    case REDO -> {
                        if (undoManager.canRedo()) {
                            undoManager.redo();
                        }
                    }
                    default -> LOGGER.debug("Only cut/copy/paste/deleteEntry supported but got: {} and focus owner {}", action, focusOwner);
                }
            }
        });
    }
}
