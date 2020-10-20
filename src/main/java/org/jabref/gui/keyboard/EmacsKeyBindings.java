package org.jabref.gui.keyboard;

import java.util.Optional;

import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.Globals;
import org.jabref.logic.util.strings.EmacsStringManipulator;
import org.jabref.model.util.ResultingEmacsState;
import org.jabref.preferences.PreferencesService;

public class EmacsKeyBindings {

    public static void executeEmacs(Scene scene, KeyEvent event, PreferencesService preferencesService) {
        if (preferencesService.getEmacsKeyPreferences().useEmacsKeyBindings()
                && scene.focusOwnerProperty().get() instanceof TextInputControl) {

            KeyBindingRepository keyBindingRepository = Globals.getKeyPrefs();
            TextInputControl focusedTextField = (TextInputControl) scene.focusOwnerProperty().get();
            Optional<KeyBinding> keyBinding = keyBindingRepository.mapToKeyBinding(event);
            keyBinding.ifPresent(binding -> {
                if (binding.equals(KeyBinding.EMACS_DELETE)) {
                    focusedTextField.deletePreviousChar();
                    event.consume();
                } else if (binding == KeyBinding.EMACS_BACKWARD) {
                    focusedTextField.backward();
                    event.consume();
                } else if (preferencesService.getEmacsKeyPreferences().shouldRebindCF()
                        && binding == KeyBinding.EMACS_FORWARD) {
                    focusedTextField.forward();
                    event.consume();
                } else if (preferencesService.getEmacsKeyPreferences().shouldRebindCA()
                        && binding == KeyBinding.EMACS_BEGINNING) {
                    focusedTextField.home();
                    event.consume();
                } else if (binding == KeyBinding.EMACS_END) {
                    focusedTextField.end();
                    event.consume();
                } else if (binding == KeyBinding.EMACS_BEGINNING_DOC) {
                    focusedTextField.home();
                    event.consume();
                } else if (binding == KeyBinding.EMACS_END_DOC) {
                    focusedTextField.end();
                    event.consume();
                } else if (binding == KeyBinding.EMACS_UP) {
                    focusedTextField.home();
                    event.consume();
                } else if (preferencesService.getEmacsKeyPreferences().shouldRebindCN()
                        && binding == KeyBinding.EMACS_DOWN) {
                    focusedTextField.end();
                    event.consume();
                } else if (binding == KeyBinding.EMACS_CAPITALIZE) {
                    int pos = focusedTextField.getCaretPosition();
                    String text = focusedTextField.getText(0, focusedTextField.getText().length());
                    ResultingEmacsState res = EmacsStringManipulator.capitalize(pos, text);
                    focusedTextField.setText(res.text);
                    focusedTextField.positionCaret(res.caretPos);
                    event.consume();
                } else if (binding == KeyBinding.EMACS_LOWERCASE) {
                    int pos = focusedTextField.getCaretPosition();
                    String text = focusedTextField.getText(0, focusedTextField.getText().length());
                    ResultingEmacsState res = EmacsStringManipulator.lowercase(pos, text);
                    focusedTextField.setText(res.text);
                    focusedTextField.positionCaret(res.caretPos);
                    event.consume();
                } else if (preferencesService.getEmacsKeyPreferences().shouldRebindAU()
                        && binding == KeyBinding.EMACS_UPPERCASE) {
                    int pos = focusedTextField.getCaretPosition();
                    String text = focusedTextField.getText(0, focusedTextField.getText().length());
                    ResultingEmacsState res = EmacsStringManipulator.uppercase(pos, text);
                    focusedTextField.setText(res.text);
                    focusedTextField.positionCaret(res.caretPos);
                    event.consume();
                } else if (binding == KeyBinding.EMACS_KILL_LINE) {
                    int pos = focusedTextField.getCaretPosition();
                    focusedTextField.setText(focusedTextField.getText(0, pos));
                    focusedTextField.positionCaret(pos);
                    event.consume();
                } else if (binding == KeyBinding.EMACS_KILL_WORD) {
                    int pos = focusedTextField.getCaretPosition();
                    String text = focusedTextField.getText(0, focusedTextField.getText().length());
                    ResultingEmacsState res = EmacsStringManipulator.killWord(pos, text);
                    focusedTextField.setText(res.text);
                    focusedTextField.positionCaret(res.caretPos);
                    event.consume();
                } else if (binding == KeyBinding.EMACS_KILL_WORD_BACKWARD) {
                    int pos = focusedTextField.getCaretPosition();
                    String text = focusedTextField.getText(0, focusedTextField.getText().length());
                    ResultingEmacsState res = EmacsStringManipulator.backwardKillWord(pos, text);
                    focusedTextField.setText(res.text);
                    focusedTextField.positionCaret(res.caretPos);
                    event.consume();
                }
            });
        }
    }
}
