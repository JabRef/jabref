package org.jabref.gui.search;

import java.util.EnumSet;
import java.util.List;

import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.SearchPreferences;
import org.jabref.testutils.category.GUITest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@GUITest
@ExtendWith(ApplicationExtension.class)
public class GlobalSearchBarTest {
    private HBox hBox;

    private StateManager stateManager;

    @Start
    public void onStart(Stage stage) {
        SearchPreferences searchPreferences = mock(SearchPreferences.class);
        when(searchPreferences.getSearchFlags()).thenReturn(EnumSet.noneOf(SearchRules.SearchFlags.class));
        PreferencesService prefs = mock(PreferencesService.class, Answers.RETURNS_DEEP_STUBS);
        when(prefs.getSearchPreferences()).thenReturn(searchPreferences);

        stateManager = new StateManager();
        // Need for active database, otherwise the searchField will be disabled
        stateManager.setActiveDatabase(new BibDatabaseContext());

        // Instantiate GlobalSearchBar class, so the change listener is registered
        GlobalSearchBar searchBar = new GlobalSearchBar(
                mock(LibraryTabContainer.class),
                stateManager,
                prefs,
                mock(CountingUndoManager.class),
                mock(DialogService.class)
        );

        hBox = new HBox(searchBar);

        Scene scene = new Scene(hBox, 400, 400);
        stage.setScene(scene);

        stage.show();
    }

    @Test
    void recordingSearchQueriesOnFocusLostOnly(FxRobot robot) throws InterruptedException {
        stateManager.clearSearchHistory();
        String searchQuery = "Smith";
        // Track the node, that the search query will be typed into
        TextInputControl searchField = robot.lookup("#searchField").queryTextInputControl();

        // The focus is on searchField node, as we click on the search box
        var searchFieldRoboto = robot.clickOn(searchField);
        for (char c : searchQuery.toCharArray()) {
            searchFieldRoboto.write(String.valueOf(c));
            Thread.sleep(401);
            assertTrue(stateManager.getWholeSearchHistory().isEmpty());
        }

        // Set the focus to another node to trigger the listener and finally record the query.
        DefaultTaskExecutor.runAndWaitInJavaFXThread(hBox::requestFocus);
        List<String> lastSearchHistory = stateManager.getWholeSearchHistory().stream().toList();

        assertEquals(List.of("Smith"), lastSearchHistory);
    }

    @Test
    void emptyQueryIsNotRecorded(FxRobot robot) {
        stateManager.clearSearchHistory();
        String searchQuery = "";
        TextInputControl searchField = robot.lookup("#searchField").queryTextInputControl();

        var searchFieldRoboto = robot.clickOn(searchField);
        searchFieldRoboto.write(searchQuery);

        DefaultTaskExecutor.runAndWaitInJavaFXThread(hBox::requestFocus);
        List<String> lastSearchHistory = stateManager.getWholeSearchHistory().stream().toList();

        assertEquals(List.of(), lastSearchHistory);
    }
}
