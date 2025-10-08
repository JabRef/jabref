package org.jabref.logic.database;
// assists the DuplicateCheckTest functionality and bibentry type manager
import java.util.List;
import java.util.Collections;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;

public class DatabaseMockTest {
    private BibDatabase mockDatabase;
    private BibEntryTypesManager mockManager;
    private BibDatabaseMode mockMode;
    private BibEntry mockEntry;

    @BeforeEach
    void setUp() {
        mockDatabase = mock(BibDatabase.class);
        mockManager = mock(BibEntryTypesManager.class, Answers.RETURNS_DEEP_STUBS);
        mockMode = mock(BibDatabaseMode.class);
        mockEntry = mock(BibEntry.class);
        when(mockManager.getAllTypes(mockMode)).thenReturn(List.of());
    }

    @Test
    void databaseMock() {
        mockDatabase.insertEntry(mockEntry);
        assertFalse(mockDatabase.containsEntryWithId(mockEntry.getId()));
        verify(mockDatabase).insertEntry(mockEntry);
    }

    @Test
    void databaseManager() {
        var result = mockManager.getAllTypes(mockMode);
        assertEquals(Collections.emptyList(), result);
        verify(mockManager).getAllTypes(mockMode);
    }
}
