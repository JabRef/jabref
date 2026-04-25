package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegExpBasedFileFinderTest {
    private static final List<String> PDF_EXTENSION = List.of("pdf");
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
    void setUp(@TempDir Path tempDir) throws IOException {
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
    void findFiles() throws IOException {
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
    void yearAuthFirstPageFindFiles() throws IOException {
        // given
        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("**/[year]_[auth]_[firstpage].*\\\\.[extension]", ',');

        // when
        List<Path> result = fileFinder.findAssociatedFiles(entry, List.of(directory), PDF_EXTENSION);
        List<Path> expected = List.of(directory.resolve("directory/subdirectory/2003_Hippel_209.pdf"));

        // then
        assertEquals(expected, result);
    }

    @Test
    void findAssociatedFilesFindFileContainingBracketsFromBracketedExpression() throws IOException {
        BibEntry bibEntry = new BibEntry().withField(StandardField.TITLE, "Regexp from [A-Z]");

        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("[TITLE]\\\\.[extension]", ',');

        List<Path> result = fileFinder.findAssociatedFiles(bibEntry, List.of(directory), PDF_EXTENSION);
        List<Path> pdfFile = List.of(directory.resolve("Regexp from [A-Z].pdf"));

        assertEquals(pdfFile, result);
    }

    @Test
    void findAssociatedFilesFindCleanedFileFromBracketedExpression() throws IOException {
        BibEntry bibEntry = new BibEntry().withField(StandardField.JOURNAL, "ACM/IEEE-CS");

        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("[JOURNAL]\\\\.[extension]", ',');

        List<Path> result = fileFinder.findAssociatedFiles(bibEntry, List.of(directory), PDF_EXTENSION);
        List<Path> pdfFile = List.of(directory.resolve("ACM_IEEE-CS.pdf"));

        assertEquals(pdfFile, result);
    }

    @Test
    void findAssociatedFilesFindFileContainingParenthesizesFromBracketedExpression() throws IOException {
        BibEntry bibEntry = new BibEntry().withCitationKey("Guo_ICC_2010")
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
    void authorWithDiacritics() throws IOException {
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
    void findFileInSubdirectory() throws IOException {
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
    void findFileNonRecursive() throws IOException {
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

    @ParameterizedTest
    @CsvSource(textBlock = """
                exact date finds exact file,        2021-07-07,            2021-07-07.pdf;2021-07.pdf, 2021-07-07.pdf, **/.*[DATE].*\\\\.[extension]
                full date falls back to month,      2021-07-07,            2021-07.pdf,               2021-07.pdf,    **/.*[DATE].*\\\\.[extension]
                full date falls back to year,       2021-07-07,            2021.pdf,                  2021.pdf,       **/.*[DATE].*\\\\.[extension]
                year+month date falls back to year, 2021-07,               2021.pdf,                  2021.pdf,       **/.*[DATE].*\\\\.[extension]
                date range uses start date,         2021-01-01/2021-12-31, 2021-01-01.pdf,            2021-01-01.pdf, **/.*[DATE].*\\\\.[extension]
                lowercase marker triggers fallback, 2021-07-07,            2021-07-07.pdf,            2021-07-07.pdf, **/.*[date].*\\\\.[extension]
                no matching file returns empty,     2021-07-07,            ,                          ,               **/.*[DATE].*\\\\.[extension]
            """)
    void dateFallbackBehavior(String description, String dateValue, String filesToCreate, String expectedFile, String pattern) throws IOException {
        // given
        BibEntry localEntry = new BibEntry(StandardEntryType.Article).withField(StandardField.DATE, dateValue);

        if (filesToCreate != null && !filesToCreate.isBlank()) {
            for (String filename : filesToCreate.split(";")) {
                Files.createFile(directory.resolve(filename.strip()));
            }
        }

        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder(pattern, ',');

        // when
        List<Path> result = fileFinder.findAssociatedFiles(localEntry, List.of(directory), PDF_EXTENSION);

        // then
        if (expectedFile == null || expectedFile.isBlank()) {
            assertEquals(List.of(), result);
        } else {
            assertEquals(List.of(directory.resolve(expectedFile)), result);
        }
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
                numeric month format,               07,    ,   2021-07.pdf
                numeric month+day format,           07,    07, 2021-07-07.pdf
                bibtex month string,                #jul#, ,   2021-07.pdf
                bibtex month string+day format,     #jul#, 07, 2021-07-07.pdf
            """)
    void dateFallbackFromYearMonthFieldsWhenNoDateField(String description, String monthValue, String dayValue, String expectedFile) throws IOException {
        // given - entry has year+month(+day) fields (no date field)
        BibEntry localEntry = new BibEntry(StandardEntryType.Article).withField(StandardField.YEAR, "2021").withField(StandardField.MONTH, monthValue);
        if (dayValue != null && !dayValue.isBlank()) {
            localEntry = localEntry.withField(StandardField.DAY, dayValue);
        }
        Files.createFile(directory.resolve(expectedFile));

        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("**/.*[DATE].*\\\\.[extension]", ',');

        // when
        List<Path> result = fileFinder.findAssociatedFiles(localEntry, List.of(directory), PDF_EXTENSION);

        // then
        assertEquals(List.of(directory.resolve(expectedFile)), result);
    }

    @Test
    void nonDatePatternUnaffectedByFallbackLogic() throws IOException {
        // given - pattern uses [YEAR] not [DATE]; fallback logic should not trigger
        BibEntry localEntry = new BibEntry(StandardEntryType.Article).withField(StandardField.YEAR, "2021");
        Files.createFile(directory.resolve("2021.pdf"));

        RegExpBasedFileFinder fileFinder = new RegExpBasedFileFinder("**/.*[YEAR].*\\\\.[extension]", ',');

        // when
        List<Path> result = fileFinder.findAssociatedFiles(localEntry, List.of(directory), PDF_EXTENSION);

        // then - existing behavior unchanged
        assertEquals(List.of(directory.resolve("2021.pdf")), result);
    }
}
