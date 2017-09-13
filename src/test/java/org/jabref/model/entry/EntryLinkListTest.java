package org.jabref.model.entry;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class EntryLinkListTest {

    private static final String key = "test";
    private static final CrossrefBib crossrefBib = new CrossrefBib();

    private BibDatabase database;
    private List<ParsedEntryLink> links;
    private ParsedEntryLink link;
    private BibEntry target;
    private BibEntry source;

    @Before
    public void before() {
        database = new BibDatabase();
        links = EntryLinkList.parse(key, database);
        link = links.get(0);
        target = null;
        source = null;
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
        target = BibEntryBuild.ing().withId("target").withCiteKey("target").now();
        source = BibEntryBuild.ing().withId("source").withCiteKey("source").crossref(target).now();
        assertSourceCrossrefsTarget();
    }

    @Test
    public void givenCrossrefBibWhenImportingCrossBibThenExpectCrossref() throws IOException {
        target = crossrefBib.getEntryByKeyOrNew("DBLP:conf/wicsa/2015");
        source = crossrefBib.getEntryByKeyOrNew("DBLP:conf/wicsa/ZimmermannWKG15");
        assertSourceCrossrefsTarget();
    }

    private void assertSourceCrossrefsTarget() {
        boolean crossrefs = false;
        Optional<String> sourceCrossref = source.getField(FieldName.CROSSREF);
        Optional<String> targetCiteKey = target.getCiteKeyOptional();
        if (sourceCrossref.isPresent()
                && targetCiteKey.isPresent()) {
            String actualcrossrefValue = sourceCrossref.get();
            String expectedCrossrefValue = targetCiteKey.get();
            crossrefs = actualcrossrefValue.contains(expectedCrossrefValue);
        }
        assertTrue(crossrefs);
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

class CrossrefBib {

    private static final ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private static final Path pathToCrossrefBib = Paths.get("src/test/resources/testbib/crossref.bib");

    private static BibDatabase database;

    CrossrefBib() {
        setDatabaseOrThrowRuntimeException();
    }

    private void setDatabaseOrThrowRuntimeException() {
        try {
            database = readDatabase(pathToCrossrefBib);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private BibDatabase readDatabase(Path path) throws IOException  {
        Reader readFromPath = Importer.getReader(path, StandardCharsets.UTF_8);
        ParserResult parsedResult = new BibtexParser(importFormatPreferences).parse(readFromPath);
        return parsedResult.getDatabase();
    }

    BibEntry getEntryByKeyOrNew(String key) {
        Optional<BibEntry> optional = database.getEntryByKey(key);
        return optional.orElse(new BibEntry());
    }
}
