package org.jabref.gui.search;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.SearchPreferences;
import org.jabref.testutils.category.GUITest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The GlobalSearchBarTest class currently serves the GUI testing regarding
 * search history recording improvement. Designed in TestFX testing framework
 * and using FxRobot utility class, in order to automate the ui testing, by
 * simulating interactions between the user and the GUI (i.e. click on
 * a node, write to a text input control etc.). The tests included verify
 * that neither redundant data are recorded to the search history nor
 * empty search queries are stored.
 *
 * @author dkokkotas
 * @version 1.0
 * @since May 2023
 */

@GUITest
@ExtendWith(ApplicationExtension.class)
public class GlobalSearchBarTest {
    private Stage stage;
    private Scene scene;
    private HBox hBox;

    private GlobalSearchBar searchBar;
    private StateManager stateManager;

    /**
     * The onStart method, accepts the stage of the application and sets
     * up the basic components of the UI (i.e. scene, layout), that will
     * be used for our tests. Also interacts with GlobalSearchBar and
     * StateManager classes, in order to set up the search box.
     *
     * @param stage Reference to the Stage class, part of the JavaFX UI toolkit
     */
    @Start
    public void onStart(Stage stage) {
        SearchPreferences searchPreferences = mock(SearchPreferences.class);
        when(searchPreferences.getSearchFlags()).thenReturn(EnumSet.noneOf(SearchRules.SearchFlags.class));
        PreferencesService prefs = mock(PreferencesService.class, Answers.RETURNS_DEEP_STUBS);
        when(prefs.getSearchPreferences()).thenReturn(searchPreferences);

        stateManager = new StateManager();
        // need for active database, otherwise the searchField will be disabled
        stateManager.setActiveDatabase(new BibDatabaseContext());

        // instantiate GlobalSearchBar class, so the change listener is registered
        searchBar = new GlobalSearchBar(
                mock(JabRefFrame.class),
                stateManager,
                prefs,
                mock(CountingUndoManager.class),
                mock(DialogService.class)
        );

        hBox = new HBox(searchBar);

        scene = new Scene(hBox, 400, 400);
        this.stage = stage;
        stage.setScene(scene);

        stage.show();
    }

    /**
     * The recordingSearchQueriesOnFocusLost method, accepts a robot reference
     * to the FxRobot class and verifies that unnecessary prefixes of the
     * search query are not recorded. Essentially, the query is recorded
     * only when the searchField node loses focus.
     *
     * @param robot Reference to the FxRobot class, a TestFx utility class
     */

    @Test
    void recordingSearchQueriesOnFocusLost(FxRobot robot) {
        stateManager.clearSearchHistory();
        String searchQuery = "Smith";
        // track the node, that the search query will be typed into
        TextInputControl searchField = robot.lookup("#searchField").queryTextInputControl();

        // the focus is on searchField node, as we click on the search box
        var searchFieldRoboto = robot.clickOn(searchField);
        for (char c : searchQuery.toCharArray()) {
            searchFieldRoboto.write(String.valueOf(c));
            try {
                Thread.sleep(401);
                Assertions.assertTrue(stateManager.getWholeSearchHistory().isEmpty());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /*
         * Set the focus to another node to trigger the listener and finally
         * record the query.
         */
        DefaultTaskExecutor.runInJavaFXThread(() -> hBox.requestFocus());
        List<String> lastSearchHistory = stateManager.getWholeSearchHistory();
        List<String> expected = List.of("Smith");

        Assertions.assertEquals(expected, lastSearchHistory);
    }

    /**
     * The avoidRecordingEmptyQuery method, accepts a robot reference to the
     * FxRobot class and verifies that no empty search queries are recorded.
     *
     * @param robot Reference to the FxRobot class, a TestFX utility class
     */

    @Test
    void avoidRecordingEmptyQuery(FxRobot robot) {
        stateManager.clearSearchHistory();
        String searchQuery = "";
        TextInputControl searchField = robot.lookup("#searchField").queryTextInputControl();

        var searchFieldRoboto = robot.clickOn(searchField);
        searchFieldRoboto.write(searchQuery);

        DefaultTaskExecutor.runInJavaFXThread(() -> hBox.requestFocus());
        List<String> lastSearchHistory = stateManager.getWholeSearchHistory();
        List<String> expected = Collections.emptyList();

        Assertions.assertEquals(expected, lastSearchHistory);
    }
}
