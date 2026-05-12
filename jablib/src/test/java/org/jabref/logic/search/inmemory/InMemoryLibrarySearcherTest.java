package org.jabref.logic.search.inmemory;

import java.util.List;
import java.util.Set;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.SearchQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryLibrarySearcherTest {

    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() {
        databaseContext = new BibDatabaseContext();
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.search.LibrarySearcherTestCases#commonSearchCases")
    void commonSearchCases(List<BibEntry> expectedMatches, SearchQuery query, List<BibEntry> entries) {
        for (BibEntry entry : entries) {
            databaseContext.getDatabase().insertEntry(entry);
        }
        List<BibEntry> matches = new InMemoryLibrarySearcher(databaseContext).getMatches(query);
        assertEquals(Set.copyOf(expectedMatches), Set.copyOf(matches));
    }
}
