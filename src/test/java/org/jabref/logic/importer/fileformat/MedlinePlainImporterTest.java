package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MedlinePlainImporterTest {

    private static final String FILE_ENDING = ".txt";
    private MedlinePlainImporter importer;

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("MedlinePlainImporterTest")
                && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    private BufferedReader readerForString(String string) {
        return new BufferedReader(new StringReader(string));
    }

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        importer = new MedlinePlainImporter(importFormatPreferences);
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.MEDLINE_PLAIN, importer.getFileType());
    }

    @Test
    void getDescription() {
        assertEquals("Importer for the MedlinePlain format.", importer.getDescription());
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void isRecognizedFormat(String fileName) throws Exception {
        ImporterTestEngine.testIsRecognizedFormat(importer, fileName);
    }

    @Test
    void doesNotRecognizeEmptyFiles() throws IOException {
        assertFalse(importer.isRecognizedFormat(readerForString("")));
    }

    @Test
    void importMultipleEntriesInSingleFile() throws IOException, URISyntaxException {
        Path inputFile = Path.of(MedlinePlainImporter.class.getResource("MedlinePlainImporterTestMultipleEntries.txt").toURI());

        List<BibEntry> entries = importer.importDatabase(inputFile).getDatabase()
                                         .getEntries();
        BibEntry testEntry = entries.getFirst();

        assertEquals(7, entries.size());
        assertEquals(StandardEntryType.Article, testEntry.getType());
        assertEquals(Optional.empty(), testEntry.getField(StandardField.MONTH));
        assertEquals(Optional.of("Long, Vicky and Marland, Hilary"), testEntry.getField(StandardField.AUTHOR));
        assertEquals(
                Optional.of(
                        "From danger and motherhood to health and beauty: health advice for the factory girl in early twentieth-century Britain."),
                testEntry.getField(StandardField.TITLE));

        testEntry = entries.get(1);
        assertEquals(StandardEntryType.Conference, testEntry.getType());
        assertEquals(Optional.of("06"), testEntry.getField(StandardField.MONTH));
        assertEquals(Optional.empty(), testEntry.getField(StandardField.AUTHOR));
        assertEquals(Optional.empty(), testEntry.getField(StandardField.TITLE));

        testEntry = entries.get(2);
        assertEquals(StandardEntryType.Book, testEntry.getType());
        assertEquals(
                Optional.of(
                        "This is a Testtitle: This title should be appended: This title should also be appended. Another append to the Title? LastTitle"),
                testEntry.getField(StandardField.TITLE));

        testEntry = entries.get(3);
        assertEquals(StandardEntryType.TechReport, testEntry.getType());
        assertTrue(testEntry.getField(StandardField.DOI).isPresent());

        testEntry = entries.get(4);
        assertEquals(StandardEntryType.InProceedings, testEntry.getType());
        assertEquals(Optional.of("Inproceedings book title"), testEntry.getField(StandardField.BOOKTITLE));

        BibEntry expectedEntry5 = new BibEntry(StandardEntryType.Proceedings)
                .withField(StandardField.KEYWORDS, "Female");
        assertEquals(expectedEntry5, entries.get(5));

        BibEntry expectedEntry6 = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.KEYWORDS, "Female");
        assertEquals(expectedEntry6, entries.get(6));
    }

    @Test
    void emptyFileImport() throws IOException {
        List<BibEntry> emptyEntries = importer.importDatabase(readerForString("")).getDatabase().getEntries();
        assertEquals(Collections.emptyList(), emptyEntries);
    }

    @ParameterizedTest
    @CsvSource({
            "MedlinePlainImporterTestCompleteEntry",
            "MedlinePlainImporterTestMultiAbstract",
            "MedlinePlainImporterTestMultiTitle",
            "MedlinePlainImporterTestDOI",
            "MedlinePlainImporterTestInproceeding"
    })
    void importSingleEntriesInSingleFiles(String testFile) throws IOException, URISyntaxException {
        String medlineFile = testFile + ".txt";
        String bibtexFile = testFile + ".bib";
        assertImportOfMedlineFileEqualsBibtexFile(medlineFile, bibtexFile);
    }

    private void assertImportOfMedlineFileEqualsBibtexFile(String medlineFile, String bibtexFile)
            throws IOException, URISyntaxException {
        Path file = Path.of(MedlinePlainImporter.class.getResource(medlineFile).toURI());

        try (InputStream nis = MedlinePlainImporter.class.getResourceAsStream(bibtexFile)) {
            List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();
            assertNotNull(entries);
            BibEntryAssert.assertEquals(nis, entries);
        }
    }

    @Test
    void multiLineComments() throws IOException {
        try (BufferedReader reader = readerForString("PMID-22664220" + "\n" + "CON - Comment1" + "\n" + "CIN - Comment2"
                + "\n" + "EIN - Comment3" + "\n" + "EFR - Comment4" + "\n" + "CRI - Comment5" + "\n" + "CRF - Comment6"
                + "\n" + "PRIN- Comment7" + "\n" + "PROF- Comment8" + "\n" + "RPI - Comment9" + "\n" + "RPF - Comment10"
                + "\n" + "RIN - Comment11" + "\n" + "ROF - Comment12" + "\n" + "UIN - Comment13" + "\n"
                + "UOF - Comment14" + "\n" + "SPIN- Comment15" + "\n" + "ORI - Comment16")) {
            List<BibEntry> actualEntries = importer.importDatabase(reader).getDatabase().getEntries();
            BibEntry expectedEntry = new BibEntry();

            expectedEntry.setField(StandardField.COMMENT,
                    "Comment1" + "\n" + "Comment2" + "\n" + "Comment3" + "\n" + "Comment4" + "\n" + "Comment5" + "\n"
                            + "Comment6" + "\n" + "Comment7" + "\n" + "Comment8" + "\n" + "Comment9" + "\n"
                            + "Comment10" + "\n" + "Comment11" + "\n" + "Comment12" + "\n" + "Comment13" + "\n"
                            + "Comment14" + "\n" + "Comment15" + "\n" + "Comment16");
            assertEquals(Collections.singletonList(expectedEntry), actualEntries);
        }
    }

    @Test
    void keyWords() throws IOException {
        try (BufferedReader reader = readerForString("""
                PMID-22664795
                MH  - Female
                OT  - Male""")) {
            List<BibEntry> actualEntries = importer.importDatabase(reader).getDatabase().getEntries();

            BibEntry expectedEntry = new BibEntry();
            expectedEntry.setField(StandardField.KEYWORDS, "Female, Male");

            assertEquals(Collections.singletonList(expectedEntry), actualEntries);
        }
    }

    @Test
    void withNbibFile() throws IOException, URISyntaxException {
        Path file = Path.of(MedlinePlainImporter.class.getResource("NbibImporterTest.nbib").toURI());

        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();

        BibEntryAssert.assertEquals(MedlinePlainImporter.class, "NbibImporterTest.bib", entries);
    }

    @Test
    void withMultipleEntries() throws IOException, URISyntaxException {
        Path file = Path.of(MedlinePlainImporter.class.getResource("MedlinePlainImporterStringOutOfBounds.txt").toURI());

        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();

        BibEntryAssert.assertEquals(MedlinePlainImporter.class, "MedlinePlainImporterStringOutOfBounds.bib", entries);
    }

    @Test
    void invalidFormat() throws URISyntaxException, IOException {
        Path file = Path.of(MedlinePlainImporter.class.getResource("MedlinePlainImporterTestInvalidFormat.xml").toURI());

        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();

        assertEquals(Collections.emptyList(), entries);
    }

    @Test
    void nullReader() throws IOException {
        Executable fail = () -> {
            try (BufferedReader reader = null) {
                importer.importDatabase(reader);
            }
        };
        assertThrows(NullPointerException.class, fail);
    }

    @Test
    void allArticleTypes() throws IOException {
        try (BufferedReader reader = readerForString("PMID-22664795" + "\n" + "MH  - Female\n" + "PT  - journal article"
                + "\n" + "PT  - classical article" + "\n" + "PT  - corrected and republished article" + "\n"
                + "PT  - introductory journal article" + "\n" + "PT  - newspaper article")) {
            List<BibEntry> actualEntries = importer.importDatabase(reader).getDatabase().getEntries();

            BibEntry expectedEntry = new BibEntry();
            expectedEntry.setType(StandardEntryType.Article);
            expectedEntry.setField(StandardField.KEYWORDS, "Female");

            assertEquals(Collections.singletonList(expectedEntry), actualEntries);
        }
    }

    @Test
    void getFormatName() {
        assertEquals("Medline/PubMed Plain", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("medlineplain", importer.getId());
    }
}
