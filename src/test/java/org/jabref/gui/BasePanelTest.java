package org.jabref.gui;

import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;

import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.maintable.MainTablePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreviewPreferences;
import org.jabref.testutils.category.GUITest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;

@GUITest
@ExtendWith(ApplicationExtension.class)
public class BasePanelTest {

    private BasePanel panel;
    private BibDatabaseContext bibDatabaseContext;
    private BasePanelPreferences preferences;

    @Start
    public void onStart(Stage stage) {
        JabRefFrame frame = mock(JabRefFrame.class, RETURNS_MOCKS);
        ExternalFileTypes externalFileTypes = mock(ExternalFileTypes.class);
        bibDatabaseContext = new BibDatabaseContext();
        preferences = new BasePanelPreferences(
                mock(MainTablePreferences.class, RETURNS_MOCKS),
                mock(AutoCompletePreferences.class, RETURNS_MOCKS),
                mock(EntryEditorPreferences.class, RETURNS_MOCKS),
                mock(KeyBindingRepository.class, RETURNS_MOCKS),
                mock(PreviewPreferences.class, RETURNS_MOCKS),
                0.5
        );
        panel = new BasePanel(frame, preferences, bibDatabaseContext, externalFileTypes);

        stage.setScene(new Scene(panel));
        stage.show();
    }

    @Test
    void dividerPositionIsRestoredOnReopenEntryEditor(FxRobot robot) throws Exception {
        BibEntry entry = new BibEntry();
        bibDatabaseContext.getDatabase().insertEntry(entry);

        SplitPane splitPane = robot.lookup(".split-pane").query();

        robot.interact(() -> panel.showAndEdit(entry));
        robot.interact(() -> splitPane.getDividers().get(0).setPosition(0.8));
        robot.sleep(1000);

        robot.interact(() -> panel.closeBottomPane());
        robot.sleep(1000);
        robot.interact(() -> panel.showAndEdit(entry));

        assertEquals(0.8, splitPane.getDividers().get(0).getPosition(), 0.1);
    }
}
