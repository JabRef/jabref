package org.jabref.gui.keyboard;

import java.util.Optional;

import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.Globals;
import org.jabref.logic.util.strings.StringManipulator;
import org.jabref.model.util.ResultingStringState;
import org.jabref.preferences.PreferencesService;

public class EditorKeyBindings {

    public static void call(Scene scene, KeyEvent event, PreferencesService preferencesService) {
        if (scene.focusOwnerProperty().get() instanceof TextInputControl) {

            KeyBindingRepository keyBindingRepository = Globals.getKeyPrefs();
            TextInputControl focusedTextField = (TextInputControl) scene.focusOwnerProperty().get();
            Optional<KeyBinding> keyBinding = keyBindingRepository.mapToKeyBinding(event);
            keyBinding.ifPresent(binding -> {
                if (binding.equals(KeyBinding.EDITOR_DELETE)) {
                    focusedTextField.deleteNextChar();
                    event.consume();
                } else if (binding == KeyBinding.EDITOR_BACKWARD) {
                    focusedTextField.backward();
                    event.consume();
                } else if (binding == KeyBinding.EDITOR_FORWARD) {
                    focusedTextField.forward();
                    event.consume();
                } else if (binding == KeyBinding.EDITOR_WORD_BACKWARD) {
                    focusedTextField.previousWord();
                    event.consume();
                } else if (binding == KeyBinding.EDITOR_WORD_FORWARD) {
                    focusedTextField.nextWord();
                    event.consume();
                } else if (binding == KeyBinding.EDITOR_BEGINNING) {
                    focusedTextField.home();
                    event.consume();
                } else if (binding == KeyBinding.EDITOR_END) {
                    focusedTextField.end();
                    event.consume();
                } else if (binding == KeyBinding.EDITOR_BEGINNING_DOC) {
                    focusedTextField.home();
                    event.consume();
                } else if (binding == KeyBinding.EDITOR_END_DOC) {
                    focusedTextField.end();
                    event.consume();
                } else if (binding == KeyBinding.EDITOR_UP) {
                    focusedTextField.home();
                    event.consume();
                } else if (binding == KeyBinding.EDITOR_DOWN) {
                    focusedTextField.end();
                    event.consume();
                } else if (binding == KeyBinding.EDITOR_CAPITALIZE) {
                    int pos = focusedTextField.getCaretPosition();
                    String text = focusedTextField.getText(0, focusedTextField.getText().length());
                    ResultingStringState res = StringManipulator.capitalize(pos, text);
                    focusedTextField.setText(res.text);
                    focusedTextField.positionCaret(res.caretPosition);
                    event.consume();
                } else if (binding == KeyBinding.EDITOR_LOWERCASE) {
                    int pos = focusedTextField.getCaretPosition();
                    String text = focusedTextField.getText(0, focusedTextField.getText().length());
                    ResultingStringState res = StringManipulator.lowercase(pos, text);
                    focusedTextField.setText(res.text);
                    focusedTextField.positionCaret(res.caretPosition);
                    event.consume();
                } else if (binding == KeyBinding.EDITOR_UPPERCASE) {
                    int pos = focusedTextField.getCaretPosition();
                    String text = focusedTextField.getText(0, focusedTextField.getText().length());
                    ResultingStringState res = StringManipulator.uppercase(pos, text);
                    focusedTextField.setText(res.text);
                    focusedTextField.positionCaret(res.caretPosition);
                    event.consume();
                } else if (binding == KeyBinding.EDITOR_KILL_LINE) {
                    int pos = focusedTextField.getCaretPosition();
                    focusedTextField.setText(focusedTextField.getText(0, pos));
                    focusedTextField.positionCaret(pos);
                    event.consume();
                } else if (binding == KeyBinding.EDITOR_KILL_WORD) {
                    int pos = focusedTextField.getCaretPosition();
                    String text = focusedTextField.getText(0, focusedTextField.getText().length());
                    ResultingStringState res = StringManipulator.killWord(pos, text);
                    focusedTextField.setText(res.text);
                    focusedTextField.positionCaret(res.caretPosition);
                    event.consume();
                } else if (binding == KeyBinding.EDITOR_KILL_WORD_BACKWARD) {
                    int pos = focusedTextField.getCaretPosition();
                    String text = focusedTextField.getText(0, focusedTextField.getText().length());
                    ResultingStringState res = StringManipulator.backwardKillWord(pos, text);
                    focusedTextField.setText(res.text);
                    focusedTextField.positionCaret(res.caretPosition);
                    event.consume();
                }
            });
        }
    }
}
