package org.jabref.logic.search.inmemory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.search.query.SearchQuery;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InMemoryLibrarySearcherTest {

    private final BibEntryPreferences bibEntryPreferences = mock(BibEntryPreferences.class);
    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() {
        when(bibEntryPreferences.getKeywordSeparator()).thenReturn(',');
        databaseContext = new BibDatabaseContext();
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.search.LibrarySearcherTestCases#commonSearchCases")
    void commonSearchCases(List<BibEntry> expectedMatches, SearchQuery query, List<BibEntry> entries) {
        for (BibEntry entry : entries) {
            databaseContext.getDatabase().insertEntry(entry);
        }
        List<BibEntry> matches = new InMemoryLibrarySearcher(databaseContext, bibEntryPreferences).getMatches(query);
        assertEquals(Set.copyOf(expectedMatches), Set.copyOf(matches));
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.search.LibrarySearcherTestCases#detailedSearchCases")
    void commonDetailedSearchCases(Map<BibEntry, MatchInformation> expectedMatches, SearchQuery query, List<BibEntry> entries) {
        for (BibEntry entry : entries) {
            databaseContext.getDatabase().insertEntry(entry);
        }
        Map<BibEntry, MatchInformation> matches = new InMemoryLibrarySearcher(databaseContext, bibEntryPreferences).getDetailedMatches(query);
        assertThat(expectedMatches.entrySet(), Matchers.everyItem(Matchers.in(matches.entrySet())));
    }
}
