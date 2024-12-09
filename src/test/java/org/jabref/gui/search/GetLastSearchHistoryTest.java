package org.jabref.gui.search;

import java.util.List;

import javafx.stage.Stage;

import org.jabref.gui.StateManager;
import org.jabref.testutils.category.GUITest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;

@GUITest
@ExtendWith(ApplicationExtension.class)
class GetLastSearchHistoryTest {
    @Start
    void onStart(Stage stage) {
        // Needed to init JavaFX thread
        stage.show();
    }

    @Test
    void getLastSearchHistory() {
        StateManager stateManager = new StateManager();
        stateManager.addSearchHistory("test1");
        stateManager.addSearchHistory("test2");
        stateManager.addSearchHistory("test3");
        List<String> lastSearchHistory = stateManager.getLastSearchHistory(2);
        List<String> expected = List.of("test2", "test3");

        assertEquals(expected, lastSearchHistory);
    }

    @Test
    void duplicateSearchHistory() {
        StateManager stateManager = new StateManager();
        stateManager.addSearchHistory("test1");
        stateManager.addSearchHistory("test2");
        stateManager.addSearchHistory("test3");
        stateManager.addSearchHistory("test1");
        List<String> lastSearchHistory = stateManager.getWholeSearchHistory();
        List<String> expected = List.of("test2", "test3", "test1");

        assertEquals(expected, lastSearchHistory);
    }

    @Test
    void clearSearchHistory() {
        StateManager stateManager = new StateManager();
        stateManager.addSearchHistory("test1");
        stateManager.addSearchHistory("test2");
        stateManager.addSearchHistory("test3");
        List<String> lastSearchHistory = stateManager.getWholeSearchHistory();
        List<String> expected = List.of("test1", "test2", "test3");
        assertEquals(expected, lastSearchHistory);
        stateManager.clearSearchHistory();
        lastSearchHistory = stateManager.getWholeSearchHistory();
        expected = List.of();
        assertEquals(expected, lastSearchHistory);
    }
}
