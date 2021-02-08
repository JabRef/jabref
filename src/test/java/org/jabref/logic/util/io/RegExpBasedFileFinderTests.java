package org.jabref.logic.util.io;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegExpBasedFileFinderTests {
    private static final List<String> PDF_EXTENSION = Collections.singletonList("pdf");
    private static final List<String> FILE_NAMES = List.of(
            "ACM_IEEE-CS.pdf",
            "pdfInDatabase.pdf",
            "Regexp from [A-Z].pdf",
            "directory/subdirectory/2003_Hippel_209.pdf",
            "directory/subdirectory/2017_Gražulis_726.pdf",
            "directory/subdirectory/pdfInSubdirectory.pdf",
            "directory/subdirectory/GUO ea - INORG CHEM COMMUN 2010 - Ferroelectric Metal Organic Framework (MOF).pdf"
            );
    private Path directory;
    private BibEntry entry;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setCitationKey("HipKro03");
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

        // Create default directories and files
        directory = tempDir;
        Files.createDirectories(directory.resolve("directory/subdirectory"));
        for (String fileName : FILE_NAMES) {
            Files.createFile(directory.resolve(fileName));
        }
    }

    @Test
    void testFindFiles() throws Exception {
        // given
        BibEntry localEntry = new BibEntry(StandardEntryType.Article).withCitationKey("pdfInDatabase");

        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("**/[citationkey].*\\\\.[extension]", ',');

        // when
        List<Path> result = fileFinder.findAssociatedFiles(localEntry, List.of(directory), PDF_EXTENSION);
        List<Path> expected = List.of(directory.resolve("pdfInDatabase.pdf"));

        // then
        assertEquals(expected, result);
    }

    @Test
    void testYearAuthFirstPageFindFiles() throws Exception {
        // given
        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("**/[year]_[auth]_[firstpage].*\\\\.[extension]", ',');

        // when
        List<Path> result = fileFinder.findAssociatedFiles(entry, List.of(directory), PDF_EXTENSION);
        List<Path> expected = List.of(directory.resolve("directory/subdirectory/2003_Hippel_209.pdf"));

        // then
        assertEquals(expected, result);
    }

    @Test
    void findAssociatedFilesFindFileContainingBracketsFromBracketedExpression() throws Exception {
        var bibEntry = new BibEntry().withField(StandardField.TITLE, "Regexp from [A-Z]");

        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("[TITLE]\\\\.[extension]", ',');

        List<Path> result = fileFinder.findAssociatedFiles(bibEntry, List.of(directory), PDF_EXTENSION);
        List<Path> pdfFile = List.of(directory.resolve("Regexp from [A-Z].pdf"));

        assertEquals(pdfFile, result);
    }

    @Test
    void findAssociatedFilesFindCleanedFileFromBracketedExpression() throws Exception {
        var bibEntry = new BibEntry().withField(StandardField.JOURNAL, "ACM/IEEE-CS");

        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("[JOURNAL]\\\\.[extension]", ',');

        List<Path> result = fileFinder.findAssociatedFiles(bibEntry, List.of(directory), PDF_EXTENSION);
        List<Path> pdfFile = List.of(directory.resolve("ACM_IEEE-CS.pdf"));

        assertEquals(pdfFile, result);
    }

    @Test
    void findAssociatedFilesFindFileContainingParenthesizesFromBracketedExpression() throws Exception {
        var bibEntry = new BibEntry().withCitationKey("Guo_ICC_2010")
                                     .withField(StandardField.TITLE, "Ferroelectric Metal Organic Framework (MOF)")
                                     .withField(StandardField.AUTHOR, "Guo, M. and Cai, H.-L. and Xiong, R.-G.")
                                     .withField(StandardField.JOURNAL, "Inorganic Chemistry Communications")
                                     .withField(StandardField.YEAR, "2010");

        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("**/.*[TITLE].*\\\\.[extension]", ',');

        List<Path> result = fileFinder.findAssociatedFiles(bibEntry, List.of(directory), PDF_EXTENSION);
        List<Path> pdfFile = List.of(directory.resolve("directory/subdirectory/GUO ea - INORG CHEM COMMUN 2010 - Ferroelectric Metal Organic Framework (MOF).pdf"));

        assertEquals(pdfFile, result);
    }

    @Test
    void testAuthorWithDiacritics() throws Exception {
        // given
        BibEntry localEntry = new BibEntry(StandardEntryType.Article).withCitationKey("Grazulis2017");
        localEntry.setField(StandardField.YEAR, "2017");
        localEntry.setField(StandardField.AUTHOR, "Gražulis, Saulius and O. Kitsune");
        localEntry.setField(StandardField.PAGES, "726--729");

        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("**/[year]_[auth]_[firstpage]\\\\.[extension]", ',');

        // when
        List<Path> result = fileFinder.findAssociatedFiles(localEntry, List.of(directory), PDF_EXTENSION);
        List<Path> expected = List.of(directory.resolve("directory/subdirectory/2017_Gražulis_726.pdf"));

        // then
        assertEquals(expected, result);
    }

    @Test
    void testFindFileInSubdirectory() throws Exception {
        // given
        BibEntry localEntry = new BibEntry(StandardEntryType.Article);
        localEntry.setCitationKey("pdfInSubdirectory");
        localEntry.setField(StandardField.YEAR, "2017");

        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("**/[citationkey].*\\\\.[extension]", ',');

        // when
        List<Path> result = fileFinder.findAssociatedFiles(localEntry, List.of(directory), PDF_EXTENSION);
        List<Path> expected = List.of(directory.resolve("directory/subdirectory/pdfInSubdirectory.pdf"));

        // then
        assertEquals(expected, result);
    }

    @Test
    void testFindFileNonRecursive() throws Exception {
        // given
        BibEntry localEntry = new BibEntry(StandardEntryType.Article);
        localEntry.setCitationKey("pdfInSubdirectory");
        localEntry.setField(StandardField.YEAR, "2017");

        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("*/[citationkey].*\\\\.[extension]", ',');

        // when
        List<Path> result = fileFinder.findAssociatedFiles(localEntry, List.of(directory), PDF_EXTENSION);

        // then
        assertTrue(result.isEmpty());
    }
}
