package org.jabref.gui.keyboard;

import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.testutils.JavaFxExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(JavaFxExtension.class)
class TextInputKeyBindingsTest {

    @Test
    void beginningMovesToBeginningOfCurrentLine() {
        TextArea textArea = new TextArea("First line\nSecond line\nThird line");
        textArea.positionCaret(15);

        Scene scene = new Scene(textArea);
        textArea.requestFocus();

        KeyBindingRepository repository = new KeyBindingRepository();
        repository.put(KeyBinding.EDITOR_BEGINNING, "CTRL+A");

        KeyEvent event = new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "",
                "",
                KeyCode.A,
                false,
                true,
                false,
                false
        );

        TextInputKeyBindings.call(scene, event, repository);

        assertEquals(11, textArea.getCaretPosition());
        assertTrue(event.isConsumed());
    }

    @Test
    void endMovesToEndOfCurrentLine() {
        TextArea textArea = new TextArea("First line\nSecond line\nThird line");
        textArea.positionCaret(15);

        Scene scene = new Scene(textArea);
        textArea.requestFocus();

        KeyBindingRepository repository = new KeyBindingRepository();
        repository.put(KeyBinding.EDITOR_END, "CTRL+E");

        KeyEvent event = new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "",
                "",
                KeyCode.E,
                false,
                true,
                false,
                false
        );

        TextInputKeyBindings.call(scene, event, repository);

        assertEquals(22, textArea.getCaretPosition());
        assertTrue(event.isConsumed());
    }

    @Test
    void killLineRemovesTextOnlyToEndOfCurrentLine() {
        TextArea textArea = new TextArea("First line\nSecond line\nThird line");
        textArea.positionCaret(15);

        Scene scene = new Scene(textArea);
        textArea.requestFocus();

        KeyBindingRepository repository = new KeyBindingRepository();
        repository.put(KeyBinding.EDITOR_KILL_LINE, "CTRL+K");

        KeyEvent event = new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "",
                "",
                KeyCode.K,
                false,
                true,
                false,
                false
        );

        TextInputKeyBindings.call(scene, event, repository);

        assertEquals("First line\nSeco\nThird line", textArea.getText());
        assertEquals(15, textArea.getCaretPosition());
        assertTrue(event.isConsumed());
    }

    @Test
    void beginningMovesToStartOfSingleLineTextField() {
        TextField textField = new TextField("Single line text");
        textField.positionCaret(8);

        Scene scene = new Scene(textField);
        textField.requestFocus();

        KeyBindingRepository repository = new KeyBindingRepository();
        repository.put(KeyBinding.EDITOR_BEGINNING, "CTRL+A");

        KeyEvent event = new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "",
                "",
                KeyCode.A,
                false,
                true,
                false,
                false
        );

        TextInputKeyBindings.call(scene, event, repository);

        assertEquals(0, textField.getCaretPosition());
        assertTrue(event.isConsumed());
    }
}
