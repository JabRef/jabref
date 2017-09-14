package org.jabref.model.entry;

import org.jabref.model.database.BibDatabase;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class EntryLinkListTest {

    private static final String key = "test";

    private BibDatabase database;
    private List<ParsedEntryLink> links;
    private ParsedEntryLink link;

    @Before
    public void before() {
        database = new BibDatabase();
        links = EntryLinkList.parse(key, database);
        link = links.get(0);
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
        BibEntry target = BibEntryBuild.ing().withId("target").withCiteKey("target").now();
        BibEntry source = BibEntryBuild.ing().withId("source").withCiteKey("source").crossref(target).now();
        assertSourceCrossrefsTarget(target, source);
    }

    private void assertSourceCrossrefsTarget(BibEntry target, BibEntry source) {
        Optional<String> sourceCrossref = source.getField(FieldName.CROSSREF);
        Optional<String> targetCiteKey = target.getCiteKeyOptional();
        assertEquals(sourceCrossref, targetCiteKey);
    }
}

class BibEntryBuild {

    private String id;
    private String citeKey;
    private String crossref = "";

    static BibEntryBuild ing() {
        return new BibEntryBuild();
    }

    BibEntryBuild withId(String id) {
        this.id = id;
        return this;
    }

    BibEntryBuild withCiteKey(String citeKey) {
        this.citeKey = citeKey;
        return this;
    }

    BibEntryBuild crossref(BibEntry target) {
        this.crossref =  getCitekeyOf(target);
        return this;
    }

    private String getCitekeyOf(BibEntry target) {
        return target.getCiteKeyOptional().orElseThrow(() -> new RuntimeException("No citekey set"));
    }

    BibEntry now() {
        assert id != null;
        BibEntry bibEntry = new BibEntry(id);
        assert citeKey != null;
        bibEntry.setCiteKey(citeKey);
        bibEntry.setField(FieldName.CROSSREF, crossref);
        return bibEntry;
    }
}
