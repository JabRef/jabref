package org.jabref.preferences;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.testfx.api.FxAssert.verifyThat;

public class PreferencesEntryPreviewTest extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/java/org/jabref/gui/preferences/preview/PreviewTab.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testCheckBoxStates() {
        // Store initial states for later comparison
        boolean initialCheckBox1State = ((CheckBox) lookup("#showAsTabCheckBox").query()).isSelected();
        boolean initialCheckBox2State = ((CheckBox) lookup("#showTooltipEntryTable").query()).isSelected();

        // Verify initial states
        verifyThat(lookup("#showAsTabCheckBox"), CoreMatchers.equalTo(initialCheckBox1State));
        verifyThat(lookup("#showTooltipEntryTable"), CoreMatchers.equalTo(initialCheckBox2State));

        // Click both checkboxes
        clickOn("#showAsTabCheckBox");
        clickOn("#showTooltipEntryTable");

        // Verify states after clicks (opposite of initial states)
        verifyThat(lookup("#showAsTabCheckBox"), CoreMatchers.equalTo(!initialCheckBox1State));
        verifyThat(lookup("#showTooltipEntryTable"), CoreMatchers.equalTo(!initialCheckBox2State));
    }
}
