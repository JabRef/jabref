package org.jabref.gui.edit;

import java.util.function.Supplier;

import javafx.scene.Node;
import javafx.scene.control.TextInputControl;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.preview.PreviewViewer;

import org.fxmisc.richtext.CodeArea;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Cut, copy, paste, delete and select-all, dispatched by what currently has focus: a text control
/// edits its own text, the entry preview copies its selection, and anything else acts on the
/// entries selected in the library. The focus owner is read from [StateManager#getFocusOwner].
public class EditAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditAction.class);

    private final Supplier<LibraryTab> tabSupplier;
    private final StandardActions action;
    private final StateManager stateManager;

    public EditAction(StandardActions action, Supplier<LibraryTab> tabSupplier, StateManager stateManager) {
        this.action = action;
        this.tabSupplier = tabSupplier;
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
                    case SELECT_ALL ->
                            textInput.selectAll();
                    case COPY ->
                            textInput.copy();
                    case CUT ->
                            textInput.cut();
                    case PASTE ->
                            textInput.paste();
                    case DELETE ->
                            textInput.clear();
                    case DELETE_ENTRY ->
                            textInput.deleteNextChar();
                    default -> {
                        String message = "Only cut/copy/paste supported in TextInputControl but got " + action;
                        LOGGER.error(message);
                        throw new IllegalStateException(message);
                    }
                }
            } else if (focusOwner instanceof CodeArea) {
                LOGGER.debug("Ignoring request in CodeArea");
            } else if (findEnclosingPreview(focusOwner) instanceof PreviewViewer previewViewer) {
                if (action == StandardActions.COPY) {
                    previewViewer.copySelectionToClipBoard();
                } else {
                    // cut/paste/delete would act on the selected entry, not on the read-only preview
                    LOGGER.debug("Ignoring {} request inside the entry preview", action);
                }
            } else {
                LOGGER.debug("Else: {}", focusOwner.getClass().getSimpleName());
                // Not sure what is selected -> copy/paste/cut selected entries except for Preview and CodeArea

                switch (action) {
                    case COPY ->
                            tabSupplier.get().copyEntry();
                    case CUT ->
                            tabSupplier.get().cutEntry();
                    case PASTE ->
                            tabSupplier.get().pasteEntry();
                    case DELETE_ENTRY ->
                            tabSupplier.get().deleteEntry();
                    default ->
                            LOGGER.debug("Only cut/copy/paste/deleteEntry supported but got: {} and focus owner {}", action, focusOwner);
                }
            }
        });
    }

    private static @Nullable PreviewViewer findEnclosingPreview(Node node) {
        for (Node current = node; current != null; current = current.getParent()) {
            if (current instanceof PreviewViewer previewViewer) {
                return previewViewer;
            }
        }
        return null;
    }
}
