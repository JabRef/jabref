package net.sf.jabref.logic.search;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DatabaseSearcherTest {

    public static final SearchQuery INVALID_SEARCH_QUERY = new SearchQuery("\\asd123{}asdf", true, true);


    @Test
    public void testGetDatabaseFromMatchesEmptyDatabase() {
        BibDatabase database = new BibDatabase();
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("whatever", true, true), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testGetDatabaseFromMatchesEmptyDatabaseInvalidSearchExpression() {
        BibDatabase database = new BibDatabase();
        List<BibEntry> matches = new DatabaseSearcher(INVALID_SEARCH_QUERY, database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testGetDatabaseFromMatchesDatabaseWithEmptyEntries() {
        BibDatabase database = new BibDatabase();
        database.insertEntry(new BibEntry());
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("whatever", true, true), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testGetDatabaseFromMatchesDatabaseWithEntries() {
        BibDatabase database = new BibDatabase();
        BibEntry entry = new BibEntry();
        entry.setType("article");
        entry.setField("author", "harrer");
        database.insertEntry(entry);
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("whatever", true, true), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testGetDatabaseFromMatchesDatabaseWithEntriesWithCorrectMatch() {
        BibDatabase database = new BibDatabase();
        BibEntry entry = new BibEntry();
        entry.setType("article");
        entry.setField("author", "harrer");
        database.insertEntry(entry);
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("harrer", true, true), database).getMatches();
        assertEquals(Collections.singletonList(entry), matches);
    }
}
