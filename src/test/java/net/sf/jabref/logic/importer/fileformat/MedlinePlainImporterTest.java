package net.sf.jabref.logic.importer.fileformat;

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

import net.sf.jabref.logic.bibtex.BibEntryAssert;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.codec.Charsets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MedlinePlainImporterTest {

    private MedlinePlainImporter importer;


    private BufferedReader readerForString(String string) {
        return new BufferedReader(new StringReader(string));
    }

    @Before
    public void setUp() {
        importer = new MedlinePlainImporter();
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileExtensions.MEDLINE_PLAIN, importer.getExtensions());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Importer for the MedlinePlain format.", importer.getDescription());
    }

    @Test
    public void testIsRecognizedFormat() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("CopacImporterTest1.txt", "CopacImporterTest2.txt", "IsiImporterTest1.isi",
                "IsiImporterTestInspec.isi", "IsiImporterTestWOS.isi", "IsiImporterTestMedline.isi");
        for (String str : list) {
            Path file = Paths.get(MedlinePlainImporter.class.getResource(str).toURI());
            Assert.assertFalse(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testIsNotRecognizedFormat() throws Exception {
        List<String> list = Arrays.asList("MedlinePlainImporterTestMultipleEntries.txt",
                "MedlinePlainImporterTestCompleteEntry.txt", "MedlinePlainImporterTestMultiAbstract.txt",
                "MedlinePlainImporterTestMultiTitle.txt", "MedlinePlainImporterTestDOI.txt",
                "MedlinePlainImporterTestInproceeding.txt");
        for (String str : list) {
            Path file = Paths.get(MedlinePlainImporter.class.getResource(str).toURI());
            Assert.assertTrue(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void doesNotRecognizeEmptyFiles() throws IOException {
        Assert.assertFalse(importer.isRecognizedFormat(readerForString("")));
    }

    @Test
    public void testImportMultipleEntriesInSingleFile() throws IOException, URISyntaxException {
        Path inputFile = Paths
                .get(MedlinePlainImporter.class.getResource("MedlinePlainImporterTestMultipleEntries.txt").toURI());

        List<BibEntry> entries = importer.importDatabase(inputFile, StandardCharsets.UTF_8).getDatabase()
                .getEntries();
        assertEquals(7, entries.size());

        BibEntry testEntry = entries.get(0);
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
        Assert.assertTrue(testEntry.getField("doi").isPresent());

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
            Assert.assertNotNull(entries);
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

    @Test(expected = NullPointerException.class)
    public void testNullReader() throws IOException {
        try (BufferedReader reader = null) {
            importer.importDatabase(reader);
        }
        fail();
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
        assertEquals("MedlinePlain", importer.getName());
    }

    @Test
    public void testGetCLIId() {
        assertEquals("medlineplain", importer.getId());
    }

}
