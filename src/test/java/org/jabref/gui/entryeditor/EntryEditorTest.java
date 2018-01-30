package org.jabref.gui.entryeditor;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.logic.bibtex.LatexFieldFormatterPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;

import org.fxmisc.richtext.CodeArea;
import org.junit.Test;
import org.mockito.Answers;
import org.testfx.framework.junit.ApplicationTest;

import static org.mockito.Mockito.mock;

public class EntryEditorTest extends ApplicationTest {

    private Stage stage;
    private Scene scene;
    private CodeArea area;
    private TabPane pane;
    private SourceTab sourceTab;

    @Override
    public void start(Stage stage) throws Exception {
        area = new CodeArea();
        area.appendText("some example\n text to go here\n across a couple of \n lines....");
        JabRefPreferences preferences = mock(JabRefPreferences.class, Answers.RETURNS_DEEP_STUBS);
        sourceTab = new SourceTab(new BibDatabaseContext(), new CountingUndoManager(), new LatexFieldFormatterPreferences(), preferences, new DummyFileUpdateMonitor());
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
    public void switchingFromSourceTabDoesNotThrowException() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setField("test", "testvalue");

        // Update source editor
        interact(() -> pane.getSelectionModel().select(2));
        interact(() -> sourceTab.bindToEntry(entry));
        clickOn(1200, 500);
        interrupt(100);

        // Switch to different tab & update entry
        interact(() -> pane.getSelectionModel().select(1));
        interact(() -> stage.setWidth(600));
        interact(() -> entry.setField("test", "new value"));

        // No exception should be thrown
        interrupt(100);
    }
}
