package org.jabref.logic.util.io;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegExpBasedFileFinderTests {

    private static final String filesDirectory = "src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder";
    private BibDatabase database;
    private BibEntry entry;

    @BeforeEach
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
    public void testFindFiles() throws Exception {
        //given
        BibEntry localEntry = new BibEntry(BibtexEntryTypes.ARTICLE.getName());
        localEntry.setCiteKey("pdfInDatabase");
        localEntry.setField("year", "2001");

        List<String> extensions = Collections.singletonList("pdf");

        List<Path> dirs = Collections.singletonList(Paths.get(filesDirectory));
        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("**/[bibtexkey].*\\\\.[extension]", ',');

        //when
        List<Path> result = fileFinder.findAssociatedFiles(localEntry, dirs, extensions);

        //then
        assertEquals(Collections.singletonList(Paths.get("src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder/pdfInDatabase.pdf")),
                result);
    }

    @Test
    public void testYearAuthFirspageFindFiles() throws Exception {
        //given
        List<String> extensions = Collections.singletonList("pdf");

        List<Path> dirs = Collections.singletonList(Paths.get(filesDirectory));
        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("**/[year]_[auth]_[firstpage].*\\\\.[extension]", ',');

        //when
        List<Path> result = fileFinder.findAssociatedFiles(entry, dirs, extensions);

        //then
        assertEquals(Collections.singletonList(Paths.get("src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder/directory/subdirectory/2003_Hippel_209.pdf")),
                result);
    }

    @Test
    public void testAuthorWithDiacritics() throws Exception {
        //given
        BibEntry localEntry = new BibEntry(BibtexEntryTypes.ARTICLE.getName());
        localEntry.setCiteKey("Grazulis2017");
        localEntry.setField("year", "2017");
        localEntry.setField("author", "Gražulis, Saulius and O. Kitsune");
        localEntry.setField("pages", "726--729");

        List<String> extensions = Collections.singletonList("pdf");

        List<Path> dirs = Collections.singletonList(Paths.get(filesDirectory));
        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("**/[year]_[auth]_[firstpage]\\\\.[extension]", ',');

        //when
        List<Path> result = fileFinder.findAssociatedFiles(localEntry, dirs, extensions);

        //then
        assertEquals(Collections.singletonList(Paths.get("src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder/directory/subdirectory/2017_Gražulis_726.pdf")),
                result);
    }

    @Test
    public void testFindFileInSubdirectory() throws Exception {
        //given
        BibEntry localEntry = new BibEntry(BibtexEntryTypes.ARTICLE.getName());
        localEntry.setCiteKey("pdfInSubdirectory");
        localEntry.setField("year", "2017");

        List<String> extensions = Collections.singletonList("pdf");

        List<Path> dirs = Collections.singletonList(Paths.get(filesDirectory));
        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("**/[bibtexkey].*\\\\.[extension]", ',');

        //when
        List<Path> result = fileFinder.findAssociatedFiles(localEntry, dirs, extensions);

        //then
        assertEquals(Collections.singletonList(Paths.get("src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder/directory/subdirectory/pdfInSubdirectory.pdf")),
                result);
    }

    @Test
    public void testFindFileNonRecursive() throws Exception {
        //given
        BibEntry localEntry = new BibEntry(BibtexEntryTypes.ARTICLE.getName());
        localEntry.setCiteKey("pdfInSubdirectory");
        localEntry.setField("year", "2017");

        List<String> extensions = Collections.singletonList("pdf");

        List<Path> dirs = Collections.singletonList(Paths.get(filesDirectory));
        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("*/[bibtexkey].*\\\\.[extension]", ',');

        //when
        List<Path> result = fileFinder.findAssociatedFiles(localEntry, dirs, extensions);

        //then
        assertTrue(result.isEmpty());
    }

    @Test
    public void testExpandBrackets() {

        assertEquals("", RegExpBasedFileFinder.expandBrackets("", entry, database, ','));

        assertEquals("dropped", RegExpBasedFileFinder.expandBrackets("drop[unknownkey]ped", entry, database,
                ','));

        assertEquals("Eric von Hippel and Georg von Krogh",
                RegExpBasedFileFinder.expandBrackets("[author]", entry, database, ','));

        assertEquals("Eric von Hippel and Georg von Krogh are two famous authors.",
                RegExpBasedFileFinder.expandBrackets("[author] are two famous authors.", entry, database,
                        ','));

        assertEquals("Eric von Hippel and Georg von Krogh are two famous authors.",
                RegExpBasedFileFinder.expandBrackets("[author] are two famous authors.", entry, database,
                        ','));

        assertEquals(
                "Eric von Hippel and Georg von Krogh have published Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science in Organization Science.",
                RegExpBasedFileFinder.expandBrackets("[author] have published [fulltitle] in [journal].", entry, database,
                        ','));

        assertEquals(
                "Eric von Hippel and Georg von Krogh have published Open Source Software and the \"Private Collective\" Innovation Model: Issues for Organization Science in Organization Science.",
                RegExpBasedFileFinder.expandBrackets("[author] have published [title] in [journal].", entry, database,
                        ','));
    }

}
