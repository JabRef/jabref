package net.sf.jabref.logic.util.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RegExpFileSearchTests {

    private static final String filesDirectory = "src/test/resources/net/sf/jabref/imports/unlinkedFilesTestFolder";
    private BibDatabase database;
    private BibEntry entry;

    @Before
    public void setUp() {

        entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        entry.setCiteKey("HipKro03");
        entry.setField("author", "Eric von Hippel and Georg von Krogh");
        entry.setField("title", "Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science");
        entry.setField("journal", "Organization Science");
        entry.setField("year", "2003");
        entry.setField("volume", "14");
        entry.setField("pages", "209--223");
        entry.setField("number", "2");
        entry.setField("address", "Institute for Operations Research and the Management Sciences (INFORMS), Linthicum, Maryland, USA");
        entry.setField("doi", "http://dx.doi.org/10.1287/orsc.14.2.209.14992");
        entry.setField("issn", "1526-5455");
        entry.setField("publisher", "INFORMS");

        database = new BibDatabase();
        database.insertEntry(entry);
    }

    @Test
    public void testFindFiles() {
        //given
        List<BibEntry> entries = new ArrayList<>();
        BibEntry localEntry = new BibEntry(BibtexEntryTypes.ARTICLE.getName());
        localEntry.setCiteKey("pdfInDatabase");
        localEntry.setField("year", "2001");
        entries.add(localEntry);

        List<String> extensions = Arrays.asList("pdf");

        List<File> dirs = Arrays.asList(new File(filesDirectory));

        //when
        Map<BibEntry, List<File>> result = RegExpFileSearch.findFilesForSet(entries, extensions, dirs,
                "**/[bibtexkey].*\\\\.[extension]", ',');

        //then
        assertEquals(1, result.keySet().size());
    }

    @Test
    public void testFieldAndFormat() {
        assertEquals("Eric von Hippel and Georg von Krogh",
                RegExpFileSearch.getFieldAndFormat("[author]", entry, database, ','));

        assertEquals("Eric von Hippel and Georg von Krogh",
                RegExpFileSearch.getFieldAndFormat("author", entry, database, ','));

        assertEquals("", RegExpFileSearch.getFieldAndFormat("[unknownkey]", entry, database,
                ','));

        assertEquals("", RegExpFileSearch.getFieldAndFormat("[:]", entry, database, ','));

        assertEquals("", RegExpFileSearch.getFieldAndFormat("[:lower]", entry, database,
                ','));

        assertEquals("eric von hippel and georg von krogh",
                RegExpFileSearch.getFieldAndFormat("[author:lower]", entry, database,
                        ','));

        assertEquals("HipKro03", RegExpFileSearch.getFieldAndFormat("[bibtexkey]", entry, database,
                ','));

        assertEquals("HipKro03", RegExpFileSearch.getFieldAndFormat("[bibtexkey:]", entry, database,
                ','));
    }

    @Test
    public void testExpandBrackets() {

        assertEquals("", RegExpFileSearch.expandBrackets("", entry, database, ','));

        assertEquals("dropped", RegExpFileSearch.expandBrackets("drop[unknownkey]ped", entry, database,
                ','));

        assertEquals("Eric von Hippel and Georg von Krogh",
                RegExpFileSearch.expandBrackets("[author]", entry, database, ','));

        assertEquals("Eric von Hippel and Georg von Krogh are two famous authors.",
                RegExpFileSearch.expandBrackets("[author] are two famous authors.", entry, database,
                        ','));

        assertEquals("Eric von Hippel and Georg von Krogh are two famous authors.",
                RegExpFileSearch.expandBrackets("[author] are two famous authors.", entry, database,
                        ','));

        assertEquals(
                "Eric von Hippel and Georg von Krogh have published Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science in Organization Science.",
                RegExpFileSearch.expandBrackets("[author] have published [title] in [journal].", entry, database,
                        ','));
    }

}
