package org.jabref.gui.keyboard;

import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;

import org.jabref.logic.util.strings.StringManipulator;
import org.jabref.model.util.ResultingStringState;

public class TextInputKeyBindings {

    // [impl->req~ux.text-editing.line-boundaries~1]
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
                        int caret = focusedTextField.getCaretPosition();
                        String text = focusedTextField.getText(0, focusedTextField.getLength());
                        focusedTextField.positionCaret(text.lastIndexOf('\n', caret - 1) + 1);
                        event.consume();
                    }
                    case EDITOR_END -> {
                        int caret = focusedTextField.getCaretPosition();
                        String text = focusedTextField.getText(0, focusedTextField.getLength());
                        int lineEnd = text.indexOf('\n', caret);
                        focusedTextField.positionCaret(lineEnd == -1 ? text.length() : lineEnd);
                        event.consume();
                    }
                    case EDITOR_UP -> {
                        moveCaretUp(focusedTextField);
                        event.consume();
                    }
                    case EDITOR_BEGINNING_DOC -> {
                        focusedTextField.home();
                        event.consume();
                    }
                    case EDITOR_DOWN -> {
                        moveCaretDown(focusedTextField);
                        event.consume();
                    }
                    case EDITOR_END_DOC -> {
                        focusedTextField.end();
                        event.consume();
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
                        String text = focusedTextField.getText(0, focusedTextField.getLength());
                        int lineEnd = text.indexOf('\n', pos);
                        if (lineEnd == -1) {
                            lineEnd = text.length();
                        } else if (lineEnd == pos) {
                            lineEnd++;
                        }
                        focusedTextField.deleteText(pos, lineEnd);
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

    private static void moveCaretUp(TextInputControl control) {
        int caret = control.getCaretPosition();
        String text = control.getText(0, control.getLength());
        int lineStart = text.lastIndexOf('\n', caret - 1) + 1;
        if (lineStart == 0) {
            control.positionCaret(0);
            return;
        }

        int previousLineEnd = lineStart - 1;
        int previousLineStart = text.lastIndexOf('\n', previousLineEnd - 1) + 1;
        control.positionCaret(Math.min(previousLineStart + caret - lineStart, previousLineEnd));
    }

    private static void moveCaretDown(TextInputControl control) {
        int caret = control.getCaretPosition();
        String text = control.getText(0, control.getLength());
        int lineStart = text.lastIndexOf('\n', caret - 1) + 1;
        int lineEnd = text.indexOf('\n', caret);
        if (lineEnd == -1) {
            control.positionCaret(text.length());
            return;
        }

        int nextLineEnd = text.indexOf('\n', lineEnd + 1);
        control.positionCaret(Math.min(lineEnd + 1 + caret - lineStart, nextLineEnd == -1 ? text.length() : nextLineEnd));
    }
}
