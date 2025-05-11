package org.jabref.gui.fieldeditors;

import java.util.Arrays;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.scene.Parent;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.model.entry.BibEntry;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface FieldEditorFX {

    void bindToEntry(BibEntry entry);

    /**
     * @implNote Decided to add undoAction and redoAction as parameter instead of passing a tabSupplier, {@link org.jabref.gui.DialogService} and {@link org.jabref.gui.StateManager} to the method.
     */
    default void establishBinding(TextInputControl textInputControl, StringProperty viewModelTextProperty, KeyBindingRepository keyBindingRepository, UndoAction undoAction, RedoAction redoAction) {
        Logger logger = LoggerFactory.getLogger(FieldEditorFX.class);

        // We need to use the "global" UndoManager instead of JavaFX TextInputControls's native undo/redo handling.
        // This also prevents NPEs. See https://github.com/JabRef/jabref/issues/11420 for details.
        textInputControl.addEventFilter(KeyEvent.ANY, e -> {
            // Fix based on https://stackoverflow.com/a/37575818/873282
            if (e.getEventType() == KeyEvent.KEY_PRESSED // if not checked, it will be fired twice: once for key pressed and once for key released
                    && e.isShortcutDown()) {
                if (keyBindingRepository.matches(e, KeyBinding.UNDO)) {
                    undoAction.execute();
                    e.consume();
                } else if (keyBindingRepository.matches(e, KeyBinding.REDO)) {
                    redoAction.execute();
                    e.consume();
                }
            }
        });

        // We need some more sophisticated handling to avoid cursor jumping
        // https://github.com/JabRef/jabref/issues/5904

        EasyBind.subscribe(viewModelTextProperty, newText -> {
            // This might be triggered by save actions from a background thread, so we need to check if we are in the FX thread
            if (Platform.isFxApplicationThread()) {
                setTextAndUpdateCaretPosition(textInputControl, newText, logger);
            } else {
                UiTaskExecutor.runInJavaFXThread(() -> setTextAndUpdateCaretPosition(textInputControl, newText, logger));
            }
        });
        EasyBind.subscribe(textInputControl.textProperty(), viewModelTextProperty::set);
    }

    private void setTextAndUpdateCaretPosition(TextInputControl textInputControl, String newText, Logger logger) {
        int lastCaretPosition = textInputControl.getCaretPosition();
        logger.trace("Caret at position {}", lastCaretPosition);
        String oldText = textInputControl.getText();
        textInputControl.setText(newText);
        logger.trace("listener triggered: '{}' -> '{}'", oldText, newText);
        if (oldText == null) {
            logger.trace("Empty field");
            return;
        }
        if (newText == null) {
            logger.trace("Field cleared");
            return;
        }
        if (oldText.equals(newText)) {
            logger.trace("No change, returned.");
            return;
        }
        logger.trace("Trying to adapt...");
        // This is a special case when the text is set to a new value
        // In this case, we want to adjust the caret position
        List<String> oldValueCharacters = Arrays.asList(oldText.split(""));
        List<String> newValueCharacters = Arrays.asList(newText.split(""));
        List<AbstractDelta<String>> deltaList = DiffUtils.diff(oldValueCharacters, newValueCharacters).getDeltas();
        logger.trace("Deltas: {}", deltaList);
        AbstractDelta<String> lastDelta = null;
        for (AbstractDelta<String> delta : deltaList) {
            if (delta.getSource().getPosition() > lastCaretPosition) {
                break;
            }
            lastDelta = delta;
        }
        if (lastDelta == null) {
            // Change happened after current caret position
            // Thus, simply restore the old position
            textInputControl.positionCaret(lastCaretPosition);
        } else {
            logger.trace("Last Delta: {}", lastDelta);
            logger.trace("Last Delta source: {}", lastDelta.getSource());
            logger.trace("Last Delta target: {}", lastDelta.getTarget());
            int offset = lastDelta.getTarget().getPosition() - lastDelta.getSource().getPosition();
            logger.trace("Offset before patching: {}", offset);

            switch (lastDelta.getType()) {
                case DELETE:
                    offset -= lastDelta.getSource().size();
                    break;
                case INSERT:
                    offset += lastDelta.getTarget().size();
                    break;
                case CHANGE:
                    offset += lastDelta.getTarget().size() - lastDelta.getSource().size();
                    break;
                default:
                    break;
            }
            logger.trace("Offset after patching: {}", offset);

            int newCaretPosition = lastCaretPosition + offset;
            textInputControl.positionCaret(newCaretPosition);
            logger.trace("newCaretPosition: {}", newCaretPosition);
        }
    }

    Parent getNode();

    default void focus() {
        getNode().getChildrenUnmodifiable()
                 .stream()
                 .findFirst()
                 .orElse(getNode())
                 .requestFocus();
    }

    /**
     * Returns relative size of the field editor in terms of display space.
     * <p>
     * A value of 1 means that the editor gets exactly as much space as all other regular editors.
     * <p>
     * A value of 2 means that the editor gets twice as much space as regular editors.
     *
     * @return the relative weight of the editor in terms of display space
     */
    default double getWeight() {
        return 1;
    }
}
