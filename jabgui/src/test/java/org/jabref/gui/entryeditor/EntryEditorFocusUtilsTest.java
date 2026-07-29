package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ApplicationExtension.class)
class EntryEditorFocusUtilsTest {

    private final List<Field> restoredFields = new ArrayList<>();

    private TextField authorField;
    private TextField titleField;
    private EntryEditorFocusUtils focusUtils;

    @Start
    void onStart(Stage stage) {
        authorField = fieldFor(StandardField.AUTHOR);
        titleField = fieldFor(StandardField.TITLE);
        TabPane tabPane = new TabPane();
        stage.setScene(new Scene(new VBox(tabPane, authorField, titleField)));
        stage.show();

        focusUtils = new EntryEditorFocusUtils(tabPane, tabPane) {
            @Override
            void setFocusToField(Field field) {
                restoredFields.add(field);
            }
        };
    }

    private static TextField fieldFor(Field field) {
        TextField textField = new TextField();
        textField.setId(field.getName());
        return textField;
    }

    /// Two entry switches queued before the JavaFX event queue drains: the first restore is stale
    /// by the time it runs and must not pull focus back to the field of the entry left behind.
    @Test
    void onlyTheLastQueuedRestoreIsApplied() {
        Platform.runLater(() -> {
            authorField.requestFocus();
            focusUtils.captureFocusedField();
            focusUtils.restoreLastFocusedField();

            titleField.requestFocus();
            focusUtils.captureFocusedField();
            focusUtils.restoreLastFocusedField();
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(List.of(StandardField.TITLE), restoredFields);
    }

    @Test
    void aSingleRestoreIsApplied() {
        Platform.runLater(() -> {
            authorField.requestFocus();
            focusUtils.captureFocusedField();
            focusUtils.restoreLastFocusedField();
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(List.of(StandardField.AUTHOR), restoredFields);
    }
}
