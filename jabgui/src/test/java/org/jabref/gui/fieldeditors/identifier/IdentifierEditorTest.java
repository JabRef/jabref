package org.jabref.gui.fieldeditors.identifier;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import org.jabref.gui.fieldeditors.EditorTextField;

import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.base.NodeMatchers;

class IdentifierEditorTest extends ApplicationTest {

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
        FxAssert.verifyThat("#fetchInformationByIdentifierButton", NodeMatchers.isInvisible());

        clickOn(textField).write("10.1001/jama.2017.18444");
        FxAssert.verifyThat("#fetchInformationByIdentifierButton", NodeMatchers.isVisible());

        interact(() -> textField.setText(""));
        FxAssert.verifyThat("#fetchInformationByIdentifierButton", NodeMatchers.isInvisible());
    }
}
