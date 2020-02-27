package org.jabref.gui.keyboard;

import java.util.Optional;

import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;

import org.jabref.Globals;
import org.jabref.logic.util.strings.EmacsStringManipulator;
import org.jabref.model.util.ResultingEmacsState;
import org.jabref.preferences.JabRefPreferences;

public class EmacsKeyBindings {

    public static void executeEmacs(Scene scene, KeyEvent event) {
        boolean EmacsFlag = Globals.prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS);
        if (EmacsFlag && scene.focusOwnerProperty().get() instanceof TextInputControl) {
            boolean CAFlag = Globals.prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CA);
            boolean CFFlag = Globals.prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CF);
            boolean CNFlag = Globals.prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CN);
            boolean AUFlag = Globals.prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_AU);

            KeyBindingRepository keyBindingRepository = Globals.getKeyPrefs();
            TextInputControl focusedTextField = (TextInputControl) scene.focusOwnerProperty().get();
            Optional<KeyBinding> keyBinding = keyBindingRepository.mapToKeyBinding(event);
            if (keyBinding.isPresent()) {
                if (keyBinding.get().equals(KeyBinding.EMACS_DELETE)) {
                    focusedTextField.deletePreviousChar();
                    event.consume();
                } else if (keyBinding.get().equals(KeyBinding.EMACS_BACKWARD)) {
                    focusedTextField.backward();
                    event.consume();
                } else if (CFFlag && keyBinding.get().equals(KeyBinding.EMACS_FORWARD)) {
                    focusedTextField.forward();
                    event.consume();
                } else if (CAFlag && keyBinding.get().equals(KeyBinding.EMACS_BEGINNING)) {
                    focusedTextField.home();
                    event.consume();
                } else if (keyBinding.get().equals(KeyBinding.EMACS_END)) {
                    focusedTextField.end();
                    event.consume();
                } else if (keyBinding.get().equals(KeyBinding.EMACS_BEGINNING_DOC)) {
                    focusedTextField.home();
                    event.consume();
                } else if (keyBinding.get().equals(KeyBinding.EMACS_END_DOC)) {
                    focusedTextField.end();
                    event.consume();
                } else if (keyBinding.get().equals(KeyBinding.EMACS_UP)) {
                    focusedTextField.home();
                    event.consume();
                } else if (CNFlag && keyBinding.get().equals(KeyBinding.EMACS_DOWN)) {
                    focusedTextField.end();
                    event.consume();
                } else if (keyBinding.get().equals(KeyBinding.EMACS_CAPITALIZE)) {
                    int pos = focusedTextField.getCaretPosition();
                    String text = focusedTextField.getText(0, focusedTextField.getText().length());
                    ResultingEmacsState res = EmacsStringManipulator.capitalize(pos, text);
                    focusedTextField.setText(res.text);
                    focusedTextField.positionCaret(res.caretPos);
                    event.consume();
                }
                else if (keyBinding.get().equals(KeyBinding.EMACS_LOWERCASE)) {
                    int pos = focusedTextField.getCaretPosition();
                    String text = focusedTextField.getText(0, focusedTextField.getText().length());
                    ResultingEmacsState res = EmacsStringManipulator.lowercase(pos, text);
                    focusedTextField.setText(res.text);
                    focusedTextField.positionCaret(res.caretPos);
                    event.consume();
                }
                else if (AUFlag && keyBinding.get().equals(KeyBinding.EMACS_UPPERCASE)) {
                    int pos = focusedTextField.getCaretPosition();
                    String text = focusedTextField.getText(0, focusedTextField.getText().length());
                    ResultingEmacsState res = EmacsStringManipulator.uppercase(pos, text);
                    focusedTextField.setText(res.text);
                    focusedTextField.positionCaret(res.caretPos);
                    event.consume();
                }
                else if (keyBinding.get().equals(KeyBinding.EMACS_KILLLINE)) {
                    int pos = focusedTextField.getCaretPosition();
                    focusedTextField.setText(focusedTextField.getText(0, pos));
                    focusedTextField.positionCaret(pos);
                    event.consume();
                } else if (keyBinding.get().equals(KeyBinding.EMACS_KILLWORD)) {
                    int pos = focusedTextField.getCaretPosition();
                    String text = focusedTextField.getText(0, focusedTextField.getText().length());
                    ResultingEmacsState res = EmacsStringManipulator.killWord(pos, text);
                    focusedTextField.setText(res.text);
                    focusedTextField.positionCaret(res.caretPos);
                    event.consume();
                }
                else if (keyBinding.get().equals(KeyBinding.EMACS_BACKWARDKILLWORD)) {
                    int pos = focusedTextField.getCaretPosition();
                    String text = focusedTextField.getText(0, focusedTextField.getText().length());
                    ResultingEmacsState res = EmacsStringManipulator.backwardKillWord(pos, text);
                    focusedTextField.setText(res.text);
                    focusedTextField.positionCaret(res.caretPos);
                    event.consume();
                }
            }
        }
    }
}
