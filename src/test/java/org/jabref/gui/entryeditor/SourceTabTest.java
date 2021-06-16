package org.jabref.gui.entryeditor;

import java.util.Collections;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.testutils.category.GUITest;

import org.fxmisc.richtext.CodeArea;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@GUITest
@ExtendWith(ApplicationExtension.class)
class SourceTabTest {

    private Stage stage;
    private Scene scene;
    private CodeArea area;
    private TabPane pane;
    private SourceTab sourceTab;

    @Start
    public void onStart(Stage stage) {
        area = new CodeArea();
        area.appendText("some example\n text to go here\n across a couple of \n lines....");
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.activeSearchQueryProperty()).thenReturn(OptionalObjectProperty.empty());
        KeyBindingRepository keyBindingRepository = new KeyBindingRepository(Collections.emptyList(), Collections.emptyList());
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentFormatterPreferences())
                .thenReturn(mock(FieldContentFormatterPreferences.class));

        sourceTab = new SourceTab(new BibDatabaseContext(), new CountingUndoManager(), new FieldWriterPreferences(), importFormatPreferences, new DummyFileUpdateMonitor(), mock(DialogService.class), stateManager, keyBindingRepository);
        pane = new TabPane(
                new Tab("main area", area),
                new Tab("other tab", new Label("some text")),
                sourceTab
        );
        scene = new Scene(pane);
        this.stage = stage;

        stage.setScene(scene);
        stage.setWidth(400);
        stage.setHeight(400);
        stage.show();

        // select the area's tab
        pane.getSelectionModel().select(0);
    }

    @Test
    void switchingFromSourceTabDoesNotThrowException(FxRobot robot) throws Exception {
        BibEntry entry = new BibEntry();
        entry.setField(new UnknownField("test"), "testvalue");

        // Update source editor
        robot.interact(() -> pane.getSelectionModel().select(2));
        robot.interact(() -> sourceTab.notifyAboutFocus(entry));
        robot.clickOn(1200, 500);
        robot.interrupt(100);

        // Switch to different tab & update entry
        robot.interact(() -> pane.getSelectionModel().select(1));
        robot.interact(() -> stage.setWidth(600));
        robot.interact(() -> entry.setField(new UnknownField("test"), "new value"));

        // No exception should be thrown
        robot.interrupt(100);
    }
}
