package net.sf.jabref.logic.search;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DatabaseSearcherTest {

    public static final SearchQuery INVALID_SEARCH_QUERY = new SearchQuery("\\asd123{}asdf", true, true);

    private BibDatabase database;


    @Before
    public void setUp() {
        database = new BibDatabase();
    }

    @Test
    public void testNoMatchesFromEmptyDatabase() {
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("whatever", true, true), database).getMatches();
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
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("whatever", true, true), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testNoMatchesFromDatabaseWithArticleTypeEntry() {
        BibEntry entry = new BibEntry();
        entry.setType("article");
        entry.setField("author", "harrer");
        database.insertEntry(entry);
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("whatever", true, true), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testCorrectMatchFromDatabaseWithArticleTypeEntry() {
        BibEntry entry = new BibEntry();
        entry.setType("article");
        entry.setField("author", "harrer");
        database.insertEntry(entry);
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("harrer", true, true), database).getMatches();
        assertEquals(Collections.singletonList(entry), matches);
    }

    @Test
    public void testNoMatchesFromEmptyDatabaseWithInvalidQuery() {
        SearchQuery query = new SearchQuery("asdf[", true, true);

        DatabaseSearcher databaseSearcher = new DatabaseSearcher(query, database);

        assertEquals(Collections.emptyList(), databaseSearcher.getMatches());
    }

    @Test
    public void testCorrectMatchFromDatabaseWithIncollectionTypeEntry() {
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.INCOLLECTION);
        entry.setField("author", "tonho");
        database.insertEntry(entry);

        SearchQuery query = new SearchQuery("tonho", true, true);
        List<BibEntry> matches = new DatabaseSearcher(query, database).getMatches();

        assertEquals(Collections.singletonList(entry), matches);
    }

    @Test
    public void testNoMatchesFromDatabaseWithTwoEntries() {
        BibEntry entry = new BibEntry();
        database.insertEntry(entry);

        entry = new BibEntry();
        entry.setType(BibtexEntryTypes.INCOLLECTION);
        entry.setField("author", "tonho");
        database.insertEntry(entry);

        SearchQuery query = new SearchQuery("tonho", true, true);
        DatabaseSearcher databaseSearcher = new DatabaseSearcher(query, database);

        assertEquals(Collections.singletonList(entry), databaseSearcher.getMatches());
    }

    @Test
    public void testNoMatchesFromDabaseWithIncollectionTypeEntry() {
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.INCOLLECTION);
        entry.setField("author", "tonho");
        database.insertEntry(entry);

        SearchQuery query = new SearchQuery("asdf", true, true);
        DatabaseSearcher databaseSearcher = new DatabaseSearcher(query, database);

        assertEquals(Collections.emptyList(), databaseSearcher.getMatches());
    }

    @Test
    public void testNoMatchFromDatabaseWithEmptyEntry() {
        BibEntry entry = new BibEntry();
        database.insertEntry(entry);

        SearchQuery query = new SearchQuery("tonho", true, true);
        DatabaseSearcher databaseSearcher = new DatabaseSearcher(query, database);

        assertEquals(Collections.emptyList(), databaseSearcher.getMatches());
    }
}
