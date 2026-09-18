package org.jabref.gui.fieldeditors.identifier;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import org.jabref.gui.fieldeditors.EditorTextField;
import org.jabref.gui.testutils.JavaFxTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentifierEditorTest extends JavaFxTest {

    private Button fetchButton;
    private EditorTextField textField;

    @Override
    public void start(Stage stage) {
        fetchButton = new Button();
        fetchButton.setId("fetchInformationByIdentifierButton");
        textField = new EditorTextField();
        textField.setId("textField");
        textField.setText("");

        fetchButton.visibleProperty().bind(textField.textProperty().isNotEmpty());
        fetchButton.managedProperty().bind(fetchButton.visibleProperty());

        HBox root = new HBox(textField, fetchButton);
        stage.setScene(new Scene(root, 400, 100));
        stage.show();
    }

    @Test
    void fetchButtonTogglesVisibilityBasedOnText() {
        interact(() -> textField.setText(""));
        assertFalse(fetchButton.isVisible());

        interact(() -> textField.setText("10.1001/jama.2017.18444"));
        assertTrue(fetchButton.isVisible());

        interact(() -> textField.setText(""));
        assertFalse(fetchButton.isVisible());
    }
}
