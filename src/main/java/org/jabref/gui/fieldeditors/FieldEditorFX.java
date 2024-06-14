package org.jabref.gui.fieldeditors;

import java.util.Arrays;
import java.util.List;

import javafx.beans.property.StringProperty;
import javafx.scene.Parent;
import javafx.scene.control.TextInputControl;

import org.jabref.gui.util.ControlHelper;
import org.jabref.model.entry.BibEntry;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface FieldEditorFX {

    void bindToEntry(BibEntry entry);

    default void establishBinding(TextInputControl textInputControl, StringProperty viewModelTextProperty) {
        // We need some more sophisticated handling to avoid cursor jumping
        // https://github.com/JabRef/jabref/issues/5904

        Logger logger = LoggerFactory.getLogger(FieldEditorFX.class);

        EasyBind.subscribe(viewModelTextProperty, newText -> {
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
            if (lastDelta != null) {
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
        });
        EasyBind.subscribe(textInputControl.textProperty(), newText -> {
            viewModelTextProperty.set(newText);
        });
    }

    Parent getNode();

    default void focus() {
        getNode().getChildrenUnmodifiable()
                 .stream()
                 .findFirst()
                 .orElse(getNode())
                 .requestFocus();
    }

    default boolean childIsFocused() {
        return ControlHelper.childIsFocused(getNode());
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
