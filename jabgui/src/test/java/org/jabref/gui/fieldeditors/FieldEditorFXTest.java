package org.jabref.gui.fieldeditors;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.os.OS;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class FieldEditorFXTest extends ApplicationTest {

    private TextField textField;
    private UndoAction undoAction;
    private RedoAction redoAction;
    /// Set when a key press reaches the control's own handlers, i.e. was not consumed by the field editor's filter
    private final AtomicBoolean keyPressReachedControl = new AtomicBoolean();

    @Override
    public void start(Stage stage) {
        textField = new TextField();
        StringProperty textProperty = new SimpleStringProperty("");

        FieldEditorFX editor = new FieldEditorFX() {
            @Override
            public void bindToEntry(BibEntry entry) {
            }

            @Override
            public Parent getNode() {
                return textField;
            }
        };

        undoAction = mock(UndoAction.class);
        redoAction = mock(RedoAction.class);
        editor.establishBinding(textField, textProperty, new KeyBindingRepository(), undoAction, redoAction);
        textField.addEventHandler(KeyEvent.KEY_PRESSED, _ -> keyPressReachedControl.set(true));

        stage.setScene(new Scene(textField, 400, 100));
        stage.show();
    }

    @Test
    void openingBraceWrapsSelectedText() {
        interact(() -> {
            textField.setText("hello world");
            textField.selectRange(6, 11);
            textField.fireEvent(new KeyEvent(
                    textField, textField, KeyEvent.KEY_TYPED, "{", "{", KeyCode.UNDEFINED,
                    false, false, false, false));
        });

        assertEquals("hello {world}", textField.getText());
    }

    @Test
    void openingBraceWrapsSelectedTextWithSpaces() {
        interact(() -> {
            textField.setText("hello  world ");
            textField.selectRange(6, 13);
            textField.fireEvent(new KeyEvent(
                    textField, textField, KeyEvent.KEY_TYPED, "{", "{", KeyCode.UNDEFINED,
                    false, false, false, false));
        });

        assertEquals("hello { world }", textField.getText());
    }

    @Test
    void openingBraceWithoutSelectionInsertsNormally() {
        interact(() -> {
            textField.setText("hello");
            textField.positionCaret(5);
            textField.fireEvent(new KeyEvent(
                    textField, textField, KeyEvent.KEY_TYPED, "{", "{", KeyCode.UNDEFINED,
                    false, false, false, false));
        });

        assertEquals("hello{", textField.getText());
    }

    // [utest->req~logic.undo.text-field-shortcut~1]
    @Test
    void undoShortcutTriggersLibraryUndoInsteadOfTextControlUndo() {
        interact(() -> {
            textField.setText("hello");
            textField.fireEvent(shortcutPressed(KeyCode.Z));
        });

        verify(undoAction).execute();
        verify(redoAction, never()).execute();
        // Consumed before reaching TextInputControl's behavior, whose own undo would throw on an empty history
        assertFalse(keyPressReachedControl.get());
        assertEquals("hello", textField.getText());
    }

    // [utest->req~logic.undo.text-field-shortcut~1]
    @Test
    void redoShortcutTriggersLibraryRedo() {
        interact(() -> textField.fireEvent(shortcutPressed(KeyCode.Y)));

        verify(redoAction).execute();
        verify(undoAction, never()).execute();
        assertFalse(keyPressReachedControl.get());
    }

    private KeyEvent shortcutPressed(KeyCode code) {
        return new KeyEvent(textField, textField, KeyEvent.KEY_PRESSED, "", code.getName(), code,
                false, !OS.OS_X, false, OS.OS_X);
    }
}
