package org.jabref.model.entry;

import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntryLinkListTest {

    private static final String key = "test";

    private BibDatabase database;
    private List<ParsedEntryLink> links;
    private ParsedEntryLink link;
    private BibEntry source;
    private BibEntry target;

    @BeforeEach
    public void before() {
        database = new BibDatabase();
        links = EntryLinkList.parse(key, database);
        link = links.get(0);
        source = create("source");
        target = create("target");
    }

    private BibEntry create(String citeKey) {
        BibEntry entry = new BibEntry();
        entry.setCiteKey(citeKey);
        database.insertEntry(entry);
        return entry;
    }

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

    @Test
    public void givenTargetAndSourceWhenSourceCrossrefTargetThenSourceCrossrefsTarget() {
        source.setField(FieldName.CROSSREF, "target");
        assertSourceCrossrefsTarget(target, source);
    }

    private void assertSourceCrossrefsTarget(BibEntry target, BibEntry source) {
        Optional<String> sourceCrossref = source.getField(FieldName.CROSSREF);
        Optional<String> targetCiteKey = target.getCiteKeyOptional();
        assertEquals(sourceCrossref, targetCiteKey);
    }
}
