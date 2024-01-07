package org.jabref.logic.search;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.rules.SearchRules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatabaseSearcherTest {

    public static final SearchQuery INVALID_SEARCH_QUERY = new SearchQuery("\\asd123{}asdf", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION));

    private BibDatabase database;

    @BeforeEach
    public void setUp() {
        database = new BibDatabase();
    }

    @Test
    public void testNoMatchesFromEmptyDatabase() {
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("whatever", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION)), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testNoMatchesFromEmptyDatabaseWithInvalidSearchExpression() {
        List<BibEntry> matches = new DatabaseSearcher(INVALID_SEARCH_QUERY, database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testGetDatabaseFromMatchesDatabaseWithEmptyEntries() {
        database.insertEntry(new BibEntry());
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("whatever", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION)), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testNoMatchesFromDatabaseWithArticleTypeEntry() {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "harrer");
        database.insertEntry(entry);
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("whatever", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION)), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testCorrectMatchFromDatabaseWithArticleTypeEntry() {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "harrer");
        database.insertEntry(entry);
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("harrer", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION)), database).getMatches();
        assertEquals(Collections.singletonList(entry), matches);
    }

    @Test
    public void testNoMatchesFromEmptyDatabaseWithInvalidQuery() {
        SearchQuery query = new SearchQuery("asdf[", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION));

        DatabaseSearcher databaseSearcher = new DatabaseSearcher(query, database);

        assertEquals(Collections.emptyList(), databaseSearcher.getMatches());
    }

    @Test
    public void testCorrectMatchFromDatabaseWithIncollectionTypeEntry() {
        BibEntry entry = new BibEntry(StandardEntryType.InCollection);
        entry.setField(StandardField.AUTHOR, "tonho");
        database.insertEntry(entry);

        SearchQuery query = new SearchQuery("tonho", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION));
        List<BibEntry> matches = new DatabaseSearcher(query, database).getMatches();

        assertEquals(Collections.singletonList(entry), matches);
    }

    @Test
    public void testNoMatchesFromDatabaseWithTwoEntries() {
        BibEntry entry = new BibEntry();
        database.insertEntry(entry);

        entry = new BibEntry(StandardEntryType.InCollection);
        entry.setField(StandardField.AUTHOR, "tonho");
        database.insertEntry(entry);

        SearchQuery query = new SearchQuery("tonho", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION));
        DatabaseSearcher databaseSearcher = new DatabaseSearcher(query, database);

        assertEquals(Collections.singletonList(entry), databaseSearcher.getMatches());
    }

    @Test
    public void testNoMatchesFromDabaseWithIncollectionTypeEntry() {
        BibEntry entry = new BibEntry(StandardEntryType.InCollection);
        entry.setField(StandardField.AUTHOR, "tonho");
        database.insertEntry(entry);

        SearchQuery query = new SearchQuery("asdf", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION));
        DatabaseSearcher databaseSearcher = new DatabaseSearcher(query, database);

        assertEquals(Collections.emptyList(), databaseSearcher.getMatches());
    }

    @Test
    public void testNoMatchFromDatabaseWithEmptyEntry() {
        BibEntry entry = new BibEntry();
        database.insertEntry(entry);

        SearchQuery query = new SearchQuery("tonho", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION));
        DatabaseSearcher databaseSearcher = new DatabaseSearcher(query, database);

        assertEquals(Collections.emptyList(), databaseSearcher.getMatches());
    }
}
