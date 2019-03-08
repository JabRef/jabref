package org.jabref.gui.entryeditor;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import org.jabref.gui.FXDialogService;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.logic.bibtex.LatexFieldFormatterPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.testutils.category.GUITest;

import org.fxmisc.richtext.CodeArea;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.mockito.Mockito.mock;

@GUITest
@ExtendWith(ApplicationExtension.class)
public class SourceTabTest {

    private Stage stage;
    private Scene scene;
    private CodeArea area;
    private TabPane pane;
    private SourceTab sourceTab;

    @Start
    public void onStart(Stage stage) {
        area = new CodeArea();
        area.appendText("some example\n text to go here\n across a couple of \n lines....");
        sourceTab = new SourceTab(new BibDatabaseContext(), new CountingUndoManager(), new LatexFieldFormatterPreferences(), mock(ImportFormatPreferences.class), new DummyFileUpdateMonitor(), new FXDialogService());
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
        entry.setField("test", "testvalue");

        // Update source editor
        robot.interact(() -> pane.getSelectionModel().select(2));
        robot.interact(() -> sourceTab.bindToEntry(entry));
        robot.clickOn(1200, 500);
        robot.interrupt(100);

        // Switch to different tab & update entry
        robot.interact(() -> pane.getSelectionModel().select(1));
        robot.interact(() -> stage.setWidth(600));
        robot.interact(() -> entry.setField("test", "new value"));

        // No exception should be thrown
        robot.interrupt(100);
    }
}
