package org.jabref.model.entry;

import org.jabref.model.database.BibDatabase;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class EntryLinkListTest {

    private String key = "test";
    private BibDatabase database = new BibDatabase();
    private List<ParsedEntryLink> links = EntryLinkList.parse(key, database);
    private ParsedEntryLink link = links.get(0);

    @Test
    public void givenFieldValueAndDatabaseWhenParsingThenExpectKey() {
        assertEquals(key, link.getKey());
    }

    @Test
    public void givenFieldValueAndDatabaseWhenParsingThenExpectDataBase() {
        assertEquals(database, link.getDataBase());
    }

    @Test
    public void givenFieldValueAndDatabaseWhenParsingThenExpectEmptyLinkedEntry() {
        assertEquals(Optional.empty(), link.getLinkedEntry());
    }

    @Test
    public void givenFieldValueAndDatabaseWhenParsingThenExpectLink() {
        ParsedEntryLink expected = new ParsedEntryLink(key, database);
        assertEquals(expected, link);
    }

    @Test
    public void givenNullFieldValueAndDatabaseWhenParsingThenExpectLinksIsEmpty() {
        links = EntryLinkList.parse(null, database);
        assertTrue(links.isEmpty());
    }
}
