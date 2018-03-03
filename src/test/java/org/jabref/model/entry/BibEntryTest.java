package org.jabref.model.entry;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BibEntryTest {

    private BibEntry entry;

    @BeforeEach
    public void setUp() {
        entry = new BibEntry();
    }

    @AfterEach
    public void tearDown() {
        entry = null;
    }

    @Test
    public void notOverrideReservedFields() {
        assertThrows(IllegalArgumentException.class, () -> entry.setField(BibEntry.ID_FIELD, "somevalue"));
    }

    @Test
    public void notClearReservedFields() {
        assertThrows(IllegalArgumentException.class, () -> entry.clearField(BibEntry.ID_FIELD));
    }

    @Test
    public void getFieldIsCaseInsensitive() throws Exception {
        entry.setField("TeSt", "value");

        assertEquals(Optional.of("value"), entry.getField("tEsT"));
    }

    @Test
    public void clonedBibentryHasUniqueID() throws Exception {
        BibEntry entry = new BibEntry();
        BibEntry entryClone = (BibEntry) entry.clone();

        assertNotEquals(entry.getId(), entryClone.getId());
    }

    @Test
    public void testGetAndAddToLinkedFileList() {
        List<LinkedFile> files = entry.getFiles();
        files.add(new LinkedFile("", "", ""));
        entry.setFiles(files);
        assertEquals(Arrays.asList(new LinkedFile("", "", "")), entry.getFiles());
    }

    @Test
    public void testGetEmptyKeywords() {
        KeywordList actual = entry.getKeywords(',');

        assertEquals(new KeywordList(), actual);
    }

    @Test
    public void testGetSingleKeywords() {
        entry.addKeyword("kw", ',');
        KeywordList actual = entry.getKeywords(',');

        assertEquals(new KeywordList(new Keyword("kw")), actual);
    }

    @Test
    public void testGetKeywords() {
        entry.addKeyword("kw", ',');
        entry.addKeyword("kw2", ',');
        entry.addKeyword("kw3", ',');
        KeywordList actual = entry.getKeywords(',');

        assertEquals(new KeywordList(new Keyword("kw"), new Keyword("kw2"), new Keyword("kw3")), actual);
    }

    @Test
    public void testGetEmptyResolvedKeywords() {
        BibDatabase database = new BibDatabase();
        BibEntry entry2 = new BibEntry();
        entry.setField(FieldName.CROSSREF, "entry2");
        entry2.setCiteKey("entry2");
        database.insertEntry(entry2);
        database.insertEntry(entry);

        KeywordList actual = entry.getResolvedKeywords(',', database);

        assertEquals(new KeywordList(), actual);
    }

    @Test
    public void testGetSingleResolvedKeywords() {
        BibDatabase database = new BibDatabase();
        BibEntry entry2 = new BibEntry();
        entry.setField(FieldName.CROSSREF, "entry2");
        entry2.setCiteKey("entry2");
        entry2.addKeyword("kw", ',');
        database.insertEntry(entry2);
        database.insertEntry(entry);

        KeywordList actual = entry.getResolvedKeywords(',', database);

        assertEquals(new KeywordList(new Keyword("kw")), actual);
    }

    @Test
    public void testGetResolvedKeywords() {
        BibDatabase database = new BibDatabase();
        BibEntry entry2 = new BibEntry();
        entry.setField(FieldName.CROSSREF, "entry2");
        entry2.setCiteKey("entry2");
        entry2.addKeyword("kw", ',');
        entry2.addKeyword("kw2", ',');
        entry2.addKeyword("kw3", ',');
        database.insertEntry(entry2);
        database.insertEntry(entry);

        KeywordList actual = entry.getResolvedKeywords(',', database);

        assertEquals(new KeywordList(new Keyword("kw"), new Keyword("kw2"), new Keyword("kw3")), actual);
    }

}
