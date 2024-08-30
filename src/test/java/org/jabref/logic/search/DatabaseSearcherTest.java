package org.jabref.logic.search;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.SearchQuery;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseSearcherTest {

    private static final SearchQuery INVALID_SEARCH_QUERY = new SearchQuery("\\asd123{}asdf", EnumSet.of(SearchFlags.REGULAR_EXPRESSION));
    private static final FilePreferences FILE_PREFERENCES = mock(FilePreferences.class);
    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() {
        when(FILE_PREFERENCES.shouldFulltextIndexLinkedFiles()).thenReturn(false);
        databaseContext = new BibDatabaseContext();
    }

    @Test
    void noMatchesFromEmptyDatabase() throws IOException {
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("whatever", EnumSet.of(SearchFlags.REGULAR_EXPRESSION)), databaseContext, FILE_PREFERENCES).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    void noMatchesFromEmptyDatabaseWithInvalidSearchExpression() throws IOException {
        List<BibEntry> matches = new DatabaseSearcher(INVALID_SEARCH_QUERY, databaseContext, FILE_PREFERENCES).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    void getDatabaseFromMatchesDatabaseWithEmptyEntries() throws IOException {
        databaseContext.getDatabase().insertEntry(new BibEntry());
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("whatever", EnumSet.of(SearchFlags.REGULAR_EXPRESSION)), databaseContext, FILE_PREFERENCES).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    void noMatchesFromDatabaseWithArticleTypeEntry() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "harrer");
        databaseContext.getDatabase().insertEntry(entry);
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("whatever", EnumSet.of(SearchFlags.REGULAR_EXPRESSION)), databaseContext, FILE_PREFERENCES).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    void correctMatchFromDatabaseWithArticleTypeEntry() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "harrer");
        databaseContext.getDatabase().insertEntry(entry);
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("harrer", EnumSet.of(SearchFlags.REGULAR_EXPRESSION)), databaseContext, FILE_PREFERENCES).getMatches();
        assertEquals(Collections.singletonList(entry), matches);
    }

    @Test
    void noMatchesFromEmptyDatabaseWithInvalidQuery() throws IOException {
        SearchQuery query = new SearchQuery("asdf[", EnumSet.of(SearchFlags.REGULAR_EXPRESSION));
        DatabaseSearcher databaseSearcher = new DatabaseSearcher(query, databaseContext, FILE_PREFERENCES);
        assertEquals(Collections.emptyList(), databaseSearcher.getMatches());
    }

    @Test
    void correctMatchFromDatabaseWithInCollectionTypeEntry() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.InCollection);
        entry.setField(StandardField.AUTHOR, "tonho");
        databaseContext.getDatabase().insertEntry(entry);

        SearchQuery query = new SearchQuery("tonho", EnumSet.of(SearchFlags.REGULAR_EXPRESSION));
        List<BibEntry> matches = new DatabaseSearcher(query, databaseContext, FILE_PREFERENCES).getMatches();

        assertEquals(Collections.singletonList(entry), matches);
    }

    @Test
    void noMatchesFromDatabaseWithTwoEntries() throws IOException {
        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        entry = new BibEntry(StandardEntryType.InCollection);
        entry.setField(StandardField.AUTHOR, "tonho");
        databaseContext.getDatabase().insertEntry(entry);

        SearchQuery query = new SearchQuery("tonho", EnumSet.of(SearchFlags.REGULAR_EXPRESSION));
        DatabaseSearcher databaseSearcher = new DatabaseSearcher(query, databaseContext, FILE_PREFERENCES);

        assertEquals(Collections.singletonList(entry), databaseSearcher.getMatches());
    }

    @Test
    void noMatchesFromDatabaseWithInCollectionTypeEntry() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.InCollection);
        entry.setField(StandardField.AUTHOR, "tonho");
        databaseContext.getDatabase().insertEntry(entry);

        SearchQuery query = new SearchQuery("asdf", EnumSet.of(SearchFlags.REGULAR_EXPRESSION));
        DatabaseSearcher databaseSearcher = new DatabaseSearcher(query, databaseContext, FILE_PREFERENCES);

        assertEquals(Collections.emptyList(), databaseSearcher.getMatches());
    }

    @Test
    void noMatchFromDatabaseWithEmptyEntry() throws IOException {
        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        SearchQuery query = new SearchQuery("tonho", EnumSet.of(SearchFlags.REGULAR_EXPRESSION));
        DatabaseSearcher databaseSearcher = new DatabaseSearcher(query, databaseContext, FILE_PREFERENCES);

        assertEquals(Collections.emptyList(), databaseSearcher.getMatches());
    }
}
