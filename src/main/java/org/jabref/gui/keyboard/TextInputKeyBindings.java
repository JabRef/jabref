package org.jabref.gui.keyboard;

import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.Globals;
import org.jabref.logic.util.strings.StringManipulator;
import org.jabref.model.util.ResultingStringState;

public class TextInputKeyBindings {

    public static void call(Scene scene, KeyEvent event) {
        if (scene.focusOwnerProperty().get() instanceof TextInputControl) {
            KeyBindingRepository keyBindingRepository = Globals.getKeyPrefs();
            TextInputControl focusedTextField = (TextInputControl) scene.focusOwnerProperty().get();
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
                    case EDITOR_BEGINNING, EDITOR_UP, EDITOR_BEGINNING_DOC -> {
                        focusedTextField.home();
                        event.consume();
                    }
                    case EDITOR_END, EDITOR_DOWN, EDITOR_END_DOC -> {
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
                }
            });
        }
    }
}
