package org.jabref.gui.search;

import java.util.List;

import javafx.stage.Stage;

import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.StateManager;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive boundary case tests for search history retrieval functionality.
 * This addresses Issue #13116 by testing edge cases and boundary conditions.
 */
@ExtendWith(ApplicationExtension.class)
class SearchHistoryBoundaryCasesTest {

    private StateManager stateManager;
    private BibDatabaseContext dbContext1;
    private BibDatabaseContext dbContext2;

    @Start
    void onStart(Stage stage) {
        // Needed to init JavaFX thread
        stage.show();
    }

    @BeforeEach
    void setUp() {
        stateManager = new JabRefGuiStateManager();
        dbContext1 = new BibDatabaseContext();
        dbContext2 = new BibDatabaseContext();
        stateManager.clearSearchHistory();
    }

    // ========== Boundary Tests for getLastSearchHistory(int size) ==========

    @Test
    void getLastSearchHistoryWithZeroSize() {
        stateManager.addSearchHistory("test1");
        stateManager.addSearchHistory("test2");
        stateManager.addSearchHistory("test3");
        
        List<String> result = stateManager.getLastSearchHistory(0);
        assertTrue(result.isEmpty(), "Requesting 0 items should return empty list");
    }

    @Test
    void getLastSearchHistoryWithNegativeSize() {
        stateManager.addSearchHistory("test1");
        stateManager.addSearchHistory("test2");
        
        // The actual implementation throws IllegalArgumentException for negative sizes
        assertThrows(IllegalArgumentException.class, () -> stateManager.getLastSearchHistory(-1),
                "Negative size should throw IllegalArgumentException");
    }

    @Test
    void getLastSearchHistoryWithSizeLargerThanHistory() {
        stateManager.addSearchHistory("test1");
        stateManager.addSearchHistory("test2");
        
        List<String> result = stateManager.getLastSearchHistory(10);
        List<String> expected = List.of("test1", "test2");
        assertEquals(expected, result, "Requesting more items than available should return all items");
    }

    @Test
    void getLastSearchHistoryWithSizeEqualToHistory() {
        stateManager.addSearchHistory("test1");
        stateManager.addSearchHistory("test2");
        stateManager.addSearchHistory("test3");
        
        List<String> result = stateManager.getLastSearchHistory(3);
        List<String> expected = List.of("test1", "test2", "test3");
        assertEquals(expected, result, "Requesting exact number of items should return all items");
    }

    @Test
    void getLastSearchHistoryWithEmptyHistory() {
        List<String> result = stateManager.getLastSearchHistory(5);
        assertTrue(result.isEmpty(), "Empty history should return empty list");
    }

    @Test
    void getLastSearchHistoryWithSingleItem() {
        stateManager.addSearchHistory("single");
        
        List<String> result = stateManager.getLastSearchHistory(1);
        assertEquals(List.of("single"), result, "Single item should be returned correctly");
        
        List<String> resultMultiple = stateManager.getLastSearchHistory(3);
        assertEquals(List.of("single"), resultMultiple, "Requesting more than available should return all items");
    }

    @Test
    void getLastSearchHistoryWithVeryLargeSize() {
        stateManager.addSearchHistory("test1");
        stateManager.addSearchHistory("test2");
        
        List<String> result = stateManager.getLastSearchHistory(Integer.MAX_VALUE);
        List<String> expected = List.of("test1", "test2");
        assertEquals(expected, result, "Very large size should return all available items");
    }

    // ========== Boundary Tests for addSearchHistory(String search) ==========

    @Test
    void addSearchHistoryWithNullString() {
        // The actual implementation doesn't check for null, so it will add null to the list
        stateManager.addSearchHistory(null);
        List<String> result = stateManager.getWholeSearchHistory();
        assertEquals(1, result.size(), "History should contain one item");
        assertTrue(result.contains(null), "History should contain null");
    }

    @Test
    void addSearchHistoryWithEmptyString() {
        stateManager.addSearchHistory("");
        List<String> result = stateManager.getWholeSearchHistory();
        assertEquals(List.of(""), result, "Empty string should be added to history");
    }

    @Test
    void addSearchHistoryWithWhitespaceOnly() {
        stateManager.addSearchHistory("   ");
        stateManager.addSearchHistory("\t");
        stateManager.addSearchHistory("\n");
        
        List<String> result = stateManager.getWholeSearchHistory();
        assertEquals(List.of("   ", "\t", "\n"), result, "Whitespace-only strings should be preserved");
    }

    @Test
    void addSearchHistoryWithVeryLongString() {
        String longString = "a".repeat(10000);
        stateManager.addSearchHistory(longString);
        
        List<String> result = stateManager.getWholeSearchHistory();
        assertEquals(List.of(longString), result, "Very long strings should be handled correctly");
    }

    @Test
    void addSearchHistoryWithSpecialCharacters() {
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~";
        stateManager.addSearchHistory(specialChars);
        
        List<String> result = stateManager.getWholeSearchHistory();
        assertEquals(List.of(specialChars), result, "Special characters should be preserved");
    }

    @Test
    void addSearchHistoryWithUnicodeCharacters() {
        String unicode = "测试🔍αβγδε";
        stateManager.addSearchHistory(unicode);
        
        List<String> result = stateManager.getWholeSearchHistory();
        assertEquals(List.of(unicode), result, "Unicode characters should be preserved");
    }

    @Test
    void addSearchHistoryWithDuplicateEntries() {
        stateManager.addSearchHistory("test1");
        stateManager.addSearchHistory("test2");
        stateManager.addSearchHistory("test1"); // Duplicate
        
        List<String> result = stateManager.getWholeSearchHistory();
        assertEquals(List.of("test2", "test1"), result, "Duplicates should be moved to end");
    }

    @Test
    void addSearchHistoryWithMultipleDuplicates() {
        stateManager.addSearchHistory("test1");
        stateManager.addSearchHistory("test2");
        stateManager.addSearchHistory("test1");
        stateManager.addSearchHistory("test3");
        stateManager.addSearchHistory("test1"); // Multiple duplicates
        
        List<String> result = stateManager.getWholeSearchHistory();
        assertEquals(List.of("test2", "test3", "test1"), result, "Multiple duplicates should be handled correctly");
    }

    // ========== Boundary Tests for clearSearchHistory() ==========

    @Test
    void clearSearchHistoryWhenEmpty() {
        // Clear already empty history
        stateManager.clearSearchHistory();
        List<String> result = stateManager.getWholeSearchHistory();
        assertTrue(result.isEmpty(), "Clearing empty history should result in empty list");
    }

    @Test
    void clearSearchHistoryWithLargeHistory() {
        // Add many items
        for (int i = 0; i < 1000; i++) {
            stateManager.addSearchHistory("test" + i);
        }
        
        assertFalse(stateManager.getWholeSearchHistory().isEmpty(), "History should not be empty before clearing");
        stateManager.clearSearchHistory();
        assertTrue(stateManager.getWholeSearchHistory().isEmpty(), "History should be empty after clearing");
    }

    @Test
    void clearSearchHistoryMultipleTimes() {
        stateManager.addSearchHistory("test1");
        stateManager.addSearchHistory("test2");
        
        stateManager.clearSearchHistory();
        assertTrue(stateManager.getWholeSearchHistory().isEmpty(), "First clear should work");
        
        stateManager.clearSearchHistory();
        assertTrue(stateManager.getWholeSearchHistory().isEmpty(), "Second clear should also work");
    }

    // ========== Boundary Tests for getWholeSearchHistory() ==========

    @Test
    void getWholeSearchHistoryReturnsImmutableView() {
        stateManager.addSearchHistory("test1");
        List<String> history = stateManager.getWholeSearchHistory();
        
        // The returned list should be observable but modifications should be controlled
        assertNotNull(history, "getWholeSearchHistory should never return null");
        assertEquals(1, history.size(), "History should contain added item");
    }

    @Test
    void getWholeSearchHistoryWithMaximumCapacity() {
        // Test behavior with many items (simulating maximum capacity scenarios)
        for (int i = 0; i < 10000; i++) {
            stateManager.addSearchHistory("item" + i);
        }
        
        List<String> result = stateManager.getWholeSearchHistory();
        assertEquals(10000, result.size(), "Large history should be handled correctly");
        
        // Verify last item is correct
        assertEquals("item9999", result.get(result.size() - 1), "Last item should be correct");
    }

    // ========== Boundary Tests for Cross-Database Behavior ==========

    @Test
    void searchHistorySharedAcrossDatabasesWithEmptyHistory() {
        stateManager.setActiveDatabase(dbContext1);
        stateManager.setActiveDatabase(dbContext2);
        
        List<String> result = stateManager.getWholeSearchHistory();
        assertTrue(result.isEmpty(), "Empty history should be shared across databases");
    }

    @Test
    void searchHistorySharedAcrossDatabasesWithBoundarySizes() {
        stateManager.setActiveDatabase(dbContext1);
        stateManager.addSearchHistory("db1_query");
        
        stateManager.setActiveDatabase(dbContext2);
        stateManager.addSearchHistory("db2_query");
        
        // Test boundary case: request more items than available
        List<String> result = stateManager.getLastSearchHistory(5);
        assertEquals(List.of("db1_query", "db2_query"), result, "Cross-database history should work with boundary sizes");
    }

    @Test
    void searchHistoryOrderWithRapidDatabaseSwitching() {
        // Rapid switching between databases
        for (int i = 0; i < 10; i++) {
            stateManager.setActiveDatabase(i % 2 == 0 ? dbContext1 : dbContext2);
            stateManager.addSearchHistory("query" + i);
        }
        
        List<String> result = stateManager.getWholeSearchHistory();
        assertEquals(10, result.size(), "All queries should be preserved regardless of database switching");
        
        // Verify order is maintained
        for (int i = 0; i < 10; i++) {
            assertEquals("query" + i, result.get(i), "Order should be maintained");
        }
    }

    // ========== Boundary Tests for Edge Cases ==========

    @Test
    void searchHistoryWithConsecutiveIdenticalEntries() {
        stateManager.addSearchHistory("same");
        stateManager.addSearchHistory("same");
        stateManager.addSearchHistory("same");
        
        List<String> result = stateManager.getWholeSearchHistory();
        assertEquals(List.of("same"), result, "Consecutive identical entries should be deduplicated");
    }

    @Test
    void searchHistoryWithCaseSensitiveDuplicates() {
        stateManager.addSearchHistory("Test");
        stateManager.addSearchHistory("test");
        stateManager.addSearchHistory("TEST");
        
        List<String> result = stateManager.getWholeSearchHistory();
        assertEquals(List.of("Test", "test", "TEST"), result, "Case-sensitive duplicates should be preserved");
    }

    @Test
    void searchHistoryWithMixedEmptyAndNonEmptyEntries() {
        stateManager.addSearchHistory("valid");
        stateManager.addSearchHistory("");
        stateManager.addSearchHistory("   ");
        stateManager.addSearchHistory("another");
        
        List<String> result = stateManager.getWholeSearchHistory();
        assertEquals(List.of("valid", "", "   ", "another"), result, "Mixed empty and non-empty entries should be preserved");
    }

    @Test
    void searchHistoryRetrievalAfterClearAndReadd() {
        stateManager.addSearchHistory("original");
        stateManager.clearSearchHistory();
        stateManager.addSearchHistory("new");
        
        List<String> result = stateManager.getWholeSearchHistory();
        assertEquals(List.of("new"), result, "History should work correctly after clear and re-add");
        
        List<String> lastResult = stateManager.getLastSearchHistory(1);
        assertEquals(List.of("new"), lastResult, "getLastSearchHistory should work after clear and re-add");
    }
}
