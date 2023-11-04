package org.jabref.gui.search;

import java.util.List;

import javafx.stage.Stage;

import org.jabref.gui.StateManager;
import org.jabref.testutils.category.GUITest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

@GUITest
@ExtendWith(ApplicationExtension.class)
public class GetLastSearchHistoryTest {
    @Start
    void onStart(Stage stage) {
        // Needed to init JavaFX thread
        stage.show();
    }

    @Test
    void testGetLastSearchHistory() {
        StateManager stateManager = new StateManager();
        stateManager.addSearchHistory("test1");
        stateManager.addSearchHistory("test2");
        stateManager.addSearchHistory("test3");
        List<String> lastSearchHistory = stateManager.getLastSearchHistory(2);
        List<String> expected = List.of("test2", "test3");

        Assertions.assertEquals(expected, lastSearchHistory);
    }

    @Test
    void testduplicateSearchHistory() {
        StateManager stateManager = new StateManager();
        stateManager.addSearchHistory("test1");
        stateManager.addSearchHistory("test2");
        stateManager.addSearchHistory("test3");
        stateManager.addSearchHistory("test1");
        List<String> lastSearchHistory = stateManager.getWholeSearchHistory();
        List<String> expected = List.of("test2", "test3", "test1");

        Assertions.assertEquals(expected, lastSearchHistory);
    }

    @Test
    void testclearSearchHistory() {
        StateManager stateManager = new StateManager();
        stateManager.addSearchHistory("test1");
        stateManager.addSearchHistory("test2");
        stateManager.addSearchHistory("test3");
        List<String> lastSearchHistory = stateManager.getWholeSearchHistory();
        List<String> expected = List.of("test1", "test2", "test3");
        Assertions.assertEquals(expected, lastSearchHistory);
        stateManager.clearSearchHistory();
        lastSearchHistory = stateManager.getWholeSearchHistory();
        expected = List.of();
        Assertions.assertEquals(expected, lastSearchHistory);
    }
}
