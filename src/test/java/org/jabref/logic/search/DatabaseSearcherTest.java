package org.jabref.logic.search;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.SearchQuery;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseSearcherTest {

    private static final FilePreferences FILE_PREFERENCES = mock(FilePreferences.class);
    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() {
        when(FILE_PREFERENCES.shouldFulltextIndexLinkedFiles()).thenReturn(false);
        databaseContext = new BibDatabaseContext();
    }

    @ParameterizedTest
    @MethodSource
    void testDatabaseSearcher(List<BibEntry> expectedMatches, SearchQuery query, List<BibEntry> entries) throws IOException {
        for (BibEntry entry : entries) {
            databaseContext.getDatabase().insertEntry(entry);
        }
        List<BibEntry> matches = new DatabaseSearcher(query, databaseContext, FILE_PREFERENCES).getMatches();
        assertEquals(expectedMatches, matches);
    }

    private static Stream<Arguments> testDatabaseSearcher() {
        BibEntry emptyEntry = new BibEntry();

        BibEntry articleEntry = new BibEntry(StandardEntryType.Article);
        articleEntry.setField(StandardField.AUTHOR, "harrer");

        BibEntry inCollectionEntry = new BibEntry(StandardEntryType.InCollection);
        inCollectionEntry.setField(StandardField.AUTHOR, "tonho");

        return Stream.of(
                Arguments.of(List.of(), new SearchQuery("whatever", EnumSet.noneOf(SearchFlags.class)), List.of()),
                Arguments.of(List.of(), new SearchQuery("whatever", EnumSet.noneOf(SearchFlags.class)), List.of(emptyEntry)),
                Arguments.of(List.of(), new SearchQuery("whatever", EnumSet.noneOf(SearchFlags.class)), List.of(emptyEntry, articleEntry, inCollectionEntry)),

                // invalid search syntax
                Arguments.of(List.of(), new SearchQuery("author:", EnumSet.noneOf(SearchFlags.class)), List.of(articleEntry)),

                Arguments.of(List.of(articleEntry), new SearchQuery("harrer", EnumSet.noneOf(SearchFlags.class)), List.of(articleEntry)),
                Arguments.of(List.of(), new SearchQuery("title: harrer", EnumSet.noneOf(SearchFlags.class)), List.of(articleEntry)),

                Arguments.of(List.of(inCollectionEntry), new SearchQuery("tonho", EnumSet.noneOf(SearchFlags.class)), List.of(inCollectionEntry)),
                Arguments.of(List.of(inCollectionEntry), new SearchQuery("tonho", EnumSet.noneOf(SearchFlags.class)), List.of(articleEntry, inCollectionEntry))
        );
    }
}
