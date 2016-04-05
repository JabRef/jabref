package net.sf.jabref.logic.search;

import java.util.Collections;

import net.sf.jabref.bibtex.BibtexEntryAssert;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import org.junit.Test;

import static org.junit.Assert.*;

public class DatabaseSearcherTest {

    public static final SearchQuery INVALID_SEARCH_QUERY = new SearchQuery("\\asd123{}asdf", true, true);


    @Test
    public void testGetDatabaseFromMatchesEmptyDatabase() {
        BibDatabase database = new BibDatabase();
        BibDatabase newDatabase = new DatabaseSearcher(new SearchQuery("whatever", true, true), database)
                .getDatabaseFromMatches();
        assertEquals(Collections.emptyList(), newDatabase.getEntries());
    }

    @Test
    public void testGetDatabaseFromMatchesEmptyDatabaseInvalidSearchExpression() {
        BibDatabase database = new BibDatabase();
        BibDatabase newDatabase = new DatabaseSearcher(INVALID_SEARCH_QUERY, database).getDatabaseFromMatches();
        assertEquals(Collections.emptyList(), newDatabase.getEntries());
    }

    @Test
    public void testGetDatabaseFromMatchesDatabaseWithEmptyEntries() {
        BibDatabase database = new BibDatabase();
        database.insertEntry(new BibEntry());
        BibDatabase newDatabase = new DatabaseSearcher(new SearchQuery("whatever", true, true), database)
                .getDatabaseFromMatches();
        assertEquals(Collections.emptyList(), newDatabase.getEntries());
    }

    @Test
    public void testGetDatabaseFromMatchesDatabaseWithEntries() {
        BibDatabase database = new BibDatabase();
        BibEntry entry = new BibEntry();
        entry.setType("article");
        entry.setField("author", "harrer");
        database.insertEntry(entry);
        BibDatabase newDatabase = new DatabaseSearcher(new SearchQuery("whatever", true, true), database)
                .getDatabaseFromMatches();
        assertEquals(Collections.emptyList(), newDatabase.getEntries());
    }

    @Test
    public void testGetDatabaseFromMatchesDatabaseWithEntriesWithCorrectMatch() {
        BibDatabase database = new BibDatabase();
        BibEntry entry = new BibEntry();
        entry.setType("article");
        entry.setField("author", "harrer");
        database.insertEntry(entry);
        BibDatabase newDatabase = new DatabaseSearcher(new SearchQuery("harrer", true, true), database)
                .getDatabaseFromMatches();
        BibtexEntryAssert.assertEquals(Collections.singletonList(entry), newDatabase.getEntries());
    }
}