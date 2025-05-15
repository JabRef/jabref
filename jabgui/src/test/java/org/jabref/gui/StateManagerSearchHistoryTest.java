package org.jabref.gui;

import org.jabref.model.database.BibDatabaseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StateManagerSearchHistoryTest {

    private StateManager stateManager;
    private BibDatabaseContext dbContext1;
    private BibDatabaseContext dbContext2;

    @BeforeEach
    void setUp() {
        stateManager = new StateManager();
        dbContext1 = new BibDatabaseContext();
        dbContext2 = new BibDatabaseContext();
    }

    @Test
    void searchHistory_isSharedAcrossMultipleDatabasesAndMaintainsCorrectOrder() {
        stateManager.setActiveDatabase(dbContext1);
        stateManager.addSearchHistory("queryA");

        stateManager.setActiveDatabase(dbContext2);
        stateManager.addSearchHistory("queryB");

        stateManager.setActiveDatabase(dbContext1);
        stateManager.addSearchHistory("queryC");

        List<String> expected = List.of("queryA", "queryB", "queryC");
        assertEquals(expected, stateManager.getWholeSearchHistory());
    }
}
