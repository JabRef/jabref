package org.jabref.gui.fieldeditors;

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
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class FieldEditorFXTest extends ApplicationTest {

    private TextField textField;

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

        editor.establishBinding(textField, textProperty, new KeyBindingRepository(),
                mock(UndoAction.class), mock(RedoAction.class));

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
}

