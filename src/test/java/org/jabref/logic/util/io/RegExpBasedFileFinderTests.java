package org.jabref.logic.util.io;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegExpBasedFileFinderTests {

    private static final String FILES_DIRECTORY = "src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder";
    private BibDatabase database;
    private BibEntry entry;

    @BeforeEach
    public void setUp() {

        entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setCiteKey("HipKro03");
        entry.setField(StandardField.AUTHOR, "Eric von Hippel and Georg von Krogh");
        entry.setField(StandardField.TITLE, "Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science");
        entry.setField(StandardField.JOURNAL, "Organization Science");
        entry.setField(StandardField.YEAR, "2003");
        entry.setField(StandardField.VOLUME, "14");
        entry.setField(StandardField.PAGES, "209--223");
        entry.setField(StandardField.NUMBER, "2");
        entry.setField(StandardField.ADDRESS, "Institute for Operations Research and the Management Sciences (INFORMS), Linthicum, Maryland, USA");
        entry.setField(StandardField.DOI, "http://dx.doi.org/10.1287/orsc.14.2.209.14992");
        entry.setField(StandardField.ISSN, "1526-5455");
        entry.setField(StandardField.PUBLISHER, "INFORMS");

        database = new BibDatabase();
        database.insertEntry(entry);
    }

    @Test
    public void testFindFiles() throws Exception {
        // given
        BibEntry localEntry = new BibEntry(StandardEntryType.Article);
        localEntry.setCiteKey("pdfInDatabase");
        localEntry.setField(StandardField.YEAR, "2001");

        List<String> extensions = Collections.singletonList("pdf");

        List<Path> dirs = Collections.singletonList(Path.of(FILES_DIRECTORY));
        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("**/[bibtexkey].*\\\\.[extension]", ',');

        // when
        List<Path> result = fileFinder.findAssociatedFiles(localEntry, dirs, extensions);

        // then
        assertEquals(Collections.singletonList(Path.of("src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder/pdfInDatabase.pdf")),
                result);
    }

    @Test
    public void testYearAuthFirspageFindFiles() throws Exception {
        // given
        List<String> extensions = Collections.singletonList("pdf");

        List<Path> dirs = Collections.singletonList(Path.of(FILES_DIRECTORY));
        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("**/[year]_[auth]_[firstpage].*\\\\.[extension]", ',');

        // when
        List<Path> result = fileFinder.findAssociatedFiles(entry, dirs, extensions);

        // then
        assertEquals(Collections.singletonList(Path.of("src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder/directory/subdirectory/2003_Hippel_209.pdf")),
                result);
    }

    @Test
    public void testAuthorWithDiacritics() throws Exception {
        // given
        BibEntry localEntry = new BibEntry(StandardEntryType.Article);
        localEntry.setCiteKey("Grazulis2017");
        localEntry.setField(StandardField.YEAR, "2017");
        localEntry.setField(StandardField.AUTHOR, "Gražulis, Saulius and O. Kitsune");
        localEntry.setField(StandardField.PAGES, "726--729");

        List<String> extensions = Collections.singletonList("pdf");

        List<Path> dirs = Collections.singletonList(Path.of(FILES_DIRECTORY));
        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("**/[year]_[auth]_[firstpage]\\\\.[extension]", ',');

        // when
        List<Path> result = fileFinder.findAssociatedFiles(localEntry, dirs, extensions);

        // then
        assertEquals(Collections.singletonList(Path.of("src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder/directory/subdirectory/2017_Gražulis_726.pdf")),
                result);
    }

    @Test
    public void testFindFileInSubdirectory() throws Exception {
        // given
        BibEntry localEntry = new BibEntry(StandardEntryType.Article);
        localEntry.setCiteKey("pdfInSubdirectory");
        localEntry.setField(StandardField.YEAR, "2017");

        List<String> extensions = Collections.singletonList("pdf");

        List<Path> dirs = Collections.singletonList(Path.of(FILES_DIRECTORY));
        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("**/[bibtexkey].*\\\\.[extension]", ',');

        // when
        List<Path> result = fileFinder.findAssociatedFiles(localEntry, dirs, extensions);

        // then
        assertEquals(Collections.singletonList(Path.of("src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder/directory/subdirectory/pdfInSubdirectory.pdf")),
                result);
    }

    @Test
    public void testFindFileNonRecursive() throws Exception {
        // given
        BibEntry localEntry = new BibEntry(StandardEntryType.Article);
        localEntry.setCiteKey("pdfInSubdirectory");
        localEntry.setField(StandardField.YEAR, "2017");

        List<String> extensions = Collections.singletonList("pdf");

        List<Path> dirs = Collections.singletonList(Path.of(FILES_DIRECTORY));
        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("*/[bibtexkey].*\\\\.[extension]", ',');

        // when
        List<Path> result = fileFinder.findAssociatedFiles(localEntry, dirs, extensions);

        // then
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
