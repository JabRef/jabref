package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.logic.util.FileType;
import org.jabref.model.entry.BibEntry;

import org.apache.commons.codec.Charsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MedlinePlainImporterTest {

    private MedlinePlainImporter importer;

    private static final String FILE_ENDING = ".txt";

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("MedlinePlainImporterTest")
                && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    private BufferedReader readerForString(String string) {
        return new BufferedReader(new StringReader(string));
    }

    @BeforeEach
    public void setUp() {
        importer = new MedlinePlainImporter();
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileType.MEDLINE_PLAIN, importer.getFileType());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Importer for the MedlinePlain format.", importer.getDescription());
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testIsRecognizedFormat(String fileName) throws Exception {
        ImporterTestEngine.testIsRecognizedFormat(importer, fileName);
    }

    @Test
    public void doesNotRecognizeEmptyFiles() throws IOException {
        assertFalse(importer.isRecognizedFormat(readerForString("")));
    }

    @Test
    public void testImportMultipleEntriesInSingleFile() throws IOException, URISyntaxException {
        Path inputFile = Paths
                .get(MedlinePlainImporter.class.getResource("MedlinePlainImporterTestMultipleEntries.txt").toURI());

        List<BibEntry> entries = importer.importDatabase(inputFile, StandardCharsets.UTF_8).getDatabase()
                .getEntries();
        BibEntry testEntry = entries.get(0);

        assertEquals(7, entries.size());
        assertEquals("article", testEntry.getType());
        assertEquals(Optional.empty(), testEntry.getField("month"));
        assertEquals(Optional.of("Long, Vicky and Marland, Hilary"), testEntry.getField("author"));
        assertEquals(
                Optional.of(
                        "From danger and motherhood to health and beauty: health advice for the factory girl in early twentieth-century Britain."),
                testEntry.getField("title"));

        testEntry = entries.get(1);
        assertEquals("conference", testEntry.getType());
        assertEquals(Optional.of("06"), testEntry.getField("month"));
        assertEquals(Optional.empty(), testEntry.getField("author"));
        assertEquals(Optional.empty(), testEntry.getField("title"));

        testEntry = entries.get(2);
        assertEquals("book", testEntry.getType());
        assertEquals(
                Optional.of(
                        "This is a Testtitle: This title should be appended: This title should also be appended. Another append to the Title? LastTitle"),
                testEntry.getField("title"));

        testEntry = entries.get(3);
        assertEquals("techreport", testEntry.getType());
        assertTrue(testEntry.getField("doi").isPresent());

        testEntry = entries.get(4);
        assertEquals("inproceedings", testEntry.getType());
        assertEquals(Optional.of("Inproceedings book title"), testEntry.getField("booktitle"));

        BibEntry expectedEntry5 = new BibEntry();
        expectedEntry5.setType("proceedings");
        expectedEntry5.setField("keywords", "Female");
        assertEquals(expectedEntry5, entries.get(5));

        BibEntry expectedEntry6 = new BibEntry();
        expectedEntry6.setType("misc");
        expectedEntry6.setField("keywords", "Female");
        assertEquals(expectedEntry6, entries.get(6));
    }

    @Test
    public void testEmptyFileImport() throws IOException {
        List<BibEntry> emptyEntries = importer.importDatabase(readerForString("")).getDatabase().getEntries();

        assertEquals(Collections.emptyList(), emptyEntries);
    }

    @Test
    public void testImportSingleEntriesInSingleFiles() throws IOException, URISyntaxException {
        List<String> testFiles = Arrays.asList("MedlinePlainImporterTestCompleteEntry",
                "MedlinePlainImporterTestMultiAbstract", "MedlinePlainImporterTestMultiTitle",
                "MedlinePlainImporterTestDOI", "MedlinePlainImporterTestInproceeding");

        for (String testFile : testFiles) {
            String medlineFile = testFile + ".txt";
            String bibtexFile = testFile + ".bib";
            assertImportOfMedlineFileEqualsBibtexFile(medlineFile, bibtexFile);
        }
    }

    private void assertImportOfMedlineFileEqualsBibtexFile(String medlineFile, String bibtexFile)
            throws IOException, URISyntaxException {
        Path file = Paths.get(MedlinePlainImporter.class.getResource(medlineFile).toURI());

        try (InputStream nis = MedlinePlainImporter.class.getResourceAsStream(bibtexFile)) {
            List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
            assertNotNull(entries);
            assertEquals(1, entries.size());
            BibEntryAssert.assertEquals(nis, entries.get(0));
        }
    }

    @Test
    public void testMultiLineComments() throws IOException {
        try (BufferedReader reader = readerForString("PMID-22664220" + "\n" + "CON - Comment1" + "\n" + "CIN - Comment2"
                + "\n" + "EIN - Comment3" + "\n" + "EFR - Comment4" + "\n" + "CRI - Comment5" + "\n" + "CRF - Comment6"
                + "\n" + "PRIN- Comment7" + "\n" + "PROF- Comment8" + "\n" + "RPI - Comment9" + "\n" + "RPF - Comment10"
                + "\n" + "RIN - Comment11" + "\n" + "ROF - Comment12" + "\n" + "UIN - Comment13" + "\n"
                + "UOF - Comment14" + "\n" + "SPIN- Comment15" + "\n" + "ORI - Comment16")) {
            List<BibEntry> actualEntries = importer.importDatabase(reader).getDatabase().getEntries();
            BibEntry expectedEntry = new BibEntry();

            expectedEntry.setField("comment",
                    "Comment1" + "\n" + "Comment2" + "\n" + "Comment3" + "\n" + "Comment4" + "\n" + "Comment5" + "\n"
                            + "Comment6" + "\n" + "Comment7" + "\n" + "Comment8" + "\n" + "Comment9" + "\n"
                            + "Comment10" + "\n" + "Comment11" + "\n" + "Comment12" + "\n" + "Comment13" + "\n"
                            + "Comment14" + "\n" + "Comment15" + "\n" + "Comment16");
            assertEquals(Collections.singletonList(expectedEntry), actualEntries);
        }
    }

    @Test
    public void testKeyWords() throws IOException {
        try (BufferedReader reader = readerForString("PMID-22664795" + "\n" + "MH  - Female" + "\n" + "OT  - Male")) {
            List<BibEntry> actualEntries = importer.importDatabase(reader).getDatabase().getEntries();

            BibEntry expectedEntry = new BibEntry();
            expectedEntry.setField("keywords", "Female, Male");

            assertEquals(Collections.singletonList(expectedEntry), actualEntries);
        }
    }

    @Test
    public void testWithNbibFile() throws IOException, URISyntaxException {
        Path file = Paths.get(MedlinePlainImporter.class.getResource("NbibImporterTest.nbib").toURI());

        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        BibEntryAssert.assertEquals(MedlinePlainImporter.class, "NbibImporterTest.bib", entries);
    }

    @Test
    public void testWithMultipleEntries() throws IOException, URISyntaxException {
        Path file = Paths
                .get(MedlinePlainImporter.class.getResource("MedlinePlainImporterStringOutOfBounds.txt").toURI());

        List<BibEntry> entries = importer.importDatabase(file, Charsets.UTF_8).getDatabase().getEntries();

        BibEntryAssert.assertEquals(MedlinePlainImporter.class, "MedlinePlainImporterStringOutOfBounds.bib", entries);
    }

    @Test
    public void testInvalidFormat() throws URISyntaxException, IOException {
        Path file = Paths
                .get(MedlinePlainImporter.class.getResource("MedlinePlainImporterTestInvalidFormat.xml").toURI());

        List<BibEntry> entries = importer.importDatabase(file, Charsets.UTF_8).getDatabase().getEntries();

        assertEquals(Collections.emptyList(), entries);
    }

    @Test
    public void testNullReader() throws IOException {
        Executable fail = () -> {
            try (BufferedReader reader = null) {
                importer.importDatabase(reader);
            }
        };
        assertThrows(NullPointerException.class, fail);
    }

    @Test
    public void testAllArticleTypes() throws IOException {
        try (BufferedReader reader = readerForString("PMID-22664795" + "\n" + "MH  - Female\n" + "PT  - journal article"
                + "\n" + "PT  - classical article" + "\n" + "PT  - corrected and republished article" + "\n"
                + "PT  - introductory journal article" + "\n" + "PT  - newspaper article")) {
            List<BibEntry> actualEntries = importer.importDatabase(reader).getDatabase().getEntries();

            BibEntry expectedEntry = new BibEntry();
            expectedEntry.setType("article");
            expectedEntry.setField("keywords", "Female");

            assertEquals(Collections.singletonList(expectedEntry), actualEntries);
        }
    }

    @Test
    public void testGetFormatName() {
        assertEquals("Medline/PubMed Plain", importer.getName());
    }

    @Test
    public void testGetCLIId() {
        assertEquals("medlineplain", importer.getId());
    }
}
