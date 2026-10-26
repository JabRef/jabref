package org.jabref.gui.keyboard;

import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.control.skin.TextInputControlSkin;
import javafx.scene.input.KeyEvent;

import org.jabref.logic.util.strings.StringManipulator;
import org.jabref.model.util.ResultingStringState;

public class TextInputKeyBindings {

    // [impl->req~macos.text-editing-shortcuts~1]
    public static void call(Scene scene, KeyEvent event, KeyBindingRepository keyBindingRepository) {
        if (scene.focusOwnerProperty().get() instanceof TextInputControl focusedTextField) {
            keyBindingRepository.mapToKeyBinding(event).ifPresent(binding -> {
                switch (binding) {
                    case EDITOR_DELETE -> {
                        focusedTextField.deleteNextChar();
                        event.consume();
                    }
                    case EDITOR_BACKWARD -> {
                        focusedTextField.backward();
                        event.consume();
                    }
                    case EDITOR_FORWARD -> {
                        focusedTextField.forward();
                        event.consume();
                    }
                    case EDITOR_WORD_BACKWARD -> {
                        focusedTextField.previousWord();
                        event.consume();
                    }
                    case EDITOR_WORD_FORWARD -> {
                        focusedTextField.nextWord();
                        event.consume();
                    }
                    case EDITOR_BEGINNING -> {
                        moveCaretByLine(focusedTextField, TextInputControlSkin.Direction.BEGINNING);
                        event.consume();
                    }
                    case EDITOR_UP -> {
                        moveCaretByLine(focusedTextField, TextInputControlSkin.Direction.UP);
                        event.consume();
                    }
                    case EDITOR_BEGINNING_DOC -> {
                        focusedTextField.home();
                        event.consume();
                    }
                    case EDITOR_END -> {
                        moveCaretByLine(focusedTextField, TextInputControlSkin.Direction.END);
                        event.consume();
                    }
                    case EDITOR_DOWN -> {
                        moveCaretByLine(focusedTextField, TextInputControlSkin.Direction.DOWN);
                        event.consume();
                    }
                    case EDITOR_END_DOC -> {
                        focusedTextField.end();
                        event.consume();
                    }
                    case EDITOR_INSERT_LINE_AFTER -> {
                        if (insertLineAfterCaret(focusedTextField)) {
                            event.consume();
                        }
                    }
                    case EDITOR_TRANSPOSE_CHARACTERS -> {
                        if (focusedTextField.getSelection().getLength() == 0) {
                            transposeCharacters(focusedTextField);
                            event.consume();
                        }
                    }
                    case EDITOR_CAPITALIZE -> {
                        int pos = focusedTextField.getCaretPosition();
                        String text = focusedTextField.getText(0, focusedTextField.getText().length());
                        ResultingStringState res = StringManipulator.capitalize(pos, text);
                        focusedTextField.setText(res.text);
                        focusedTextField.positionCaret(res.caretPosition);
                        event.consume();
                    }
                    case EDITOR_LOWERCASE -> {
                        int pos = focusedTextField.getCaretPosition();
                        String text = focusedTextField.getText(0, focusedTextField.getText().length());
                        ResultingStringState res = StringManipulator.lowercase(pos, text);
                        focusedTextField.setText(res.text);
                        focusedTextField.positionCaret(res.caretPosition);
                        event.consume();
                    }
                    case EDITOR_UPPERCASE -> {
                        int pos = focusedTextField.getCaretPosition();
                        String text = focusedTextField.getText(0, focusedTextField.getText().length());
                        ResultingStringState res = StringManipulator.uppercase(pos, text);
                        focusedTextField.setText(res.text);
                        focusedTextField.positionCaret(res.caretPosition);
                        event.consume();
                    }
                    case EDITOR_KILL_LINE -> {
                        int pos = focusedTextField.getCaretPosition();
                        focusedTextField.setText(focusedTextField.getText(0, pos));
                        focusedTextField.positionCaret(pos);
                        event.consume();
                    }
                    case EDITOR_KILL_WORD -> {
                        int pos = focusedTextField.getCaretPosition();
                        String text = focusedTextField.getText(0, focusedTextField.getText().length());
                        ResultingStringState res = StringManipulator.killWord(pos, text);
                        focusedTextField.setText(res.text);
                        focusedTextField.positionCaret(res.caretPosition);
                        event.consume();
                    }
                    case EDITOR_KILL_WORD_BACKWARD -> {
                        int pos = focusedTextField.getCaretPosition();
                        String text = focusedTextField.getText(0, focusedTextField.getText().length());
                        ResultingStringState res = StringManipulator.backwardKillWord(pos, text);
                        focusedTextField.setText(res.text);
                        focusedTextField.positionCaret(res.caretPosition);
                        event.consume();
                    }
                    // CLEAR_SEARCH is handled at org.jabref.gui.search.SearchTextField
                }
            });
        }
    }

    /// Moves the caret by one visual line (UP, DOWN) or to the start/end of the current visual line (BEGINNING, END)
    /// in a multi-line [TextArea], using the public line navigation of its skin.
    /// Single-line inputs fall back to the start or end of the text, which is the same position for a single line.
    static void moveCaretByLine(TextInputControl textInput, TextInputControlSkin.Direction direction) {
        if (textInput instanceof TextArea && textInput.getSkin() instanceof TextAreaSkin skin) {
            skin.moveCaret(TextInputControlSkin.TextUnit.LINE, direction, false);
            return;
        }
        if (direction == TextInputControlSkin.Direction.UP || direction == TextInputControlSkin.Direction.BEGINNING) {
            textInput.home();
        } else if (direction == TextInputControlSkin.Direction.DOWN || direction == TextInputControlSkin.Direction.END) {
            textInput.end();
        } else {
            throw new IllegalArgumentException("Unsupported direction: " + direction);
        }
    }

    /// macOS Ctrl+O: inserts a line break at the caret without moving the caret.
    /// Only editable text areas are supported, because single-line fields cannot contain line breaks.
    ///
    /// @return true if a line break was inserted
    static boolean insertLineAfterCaret(TextInputControl textInput) {
        if (!(textInput instanceof TextArea) || !textInput.isEditable()) {
            return false;
        }
        int caret = textInput.getCaretPosition();
        textInput.insertText(caret, "\n");
        textInput.positionCaret(caret);
        return true;
    }

    static void transposeCharacters(TextInputControl textInput) {
        String text = textInput.getText();
        int caret = textInput.getCaretPosition();
        if (caret == 0 || caret >= text.length()) {
            return;
        }

        int previousCharacterStart = text.offsetByCodePoints(caret, -1);
        int nextCharacterEnd = text.offsetByCodePoints(caret, 1);
        String replacement = text.substring(caret, nextCharacterEnd) + text.substring(previousCharacterStart, caret);
        textInput.replaceText(previousCharacterStart, nextCharacterEnd, replacement);
        textInput.positionCaret(nextCharacterEnd);
    }

}
