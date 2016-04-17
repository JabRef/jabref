package net.sf.jabref.importer.fileformat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.BibtexEntryAssert;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;

public class MedlinePlainImporterTest {

    private final InputStream emptyFileStream = streamForString("");
    private MedlinePlainImporter importer;


    private InputStream streamForString(String string) {
        return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
    }

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        importer = new MedlinePlainImporter();
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        List<String> list = Arrays.asList("CopacImporterTest1.txt", "CopacImporterTest2.txt", "IsiImporterTest1.isi",
                "IsiImporterTestInspec.isi", "IsiImporterTestWOS.isi", "IsiImporterTestMedline.isi");
        for (String str : list) {
            try (InputStream is = MedlinePlainImporter.class.getResourceAsStream(str)) {
                Assert.assertFalse(importer.isRecognizedFormat(is));
            }
        }
    }

    @Test
    public void testIsNotRecognizedFormat() throws Exception {
        List<String> list = Arrays.asList("MedlinePlainImporterTestMultipleEntries.txt",
                "MedlinePlainImporterTestCompleteEntry.txt", "MedlinePlainImporterTestMultiAbstract.txt",
                "MedlinePlainImporterTestMultiTitle.txt", "MedlinePlainImporterTestDOI.txt",
                "MedlinePlainImporterTestInproceeding.txt");
        for (String str : list) {
            try (InputStream is = MedlinePlainImporter.class.getResourceAsStream(str)) {
                Assert.assertTrue(importer.isRecognizedFormat(is));
            }
        }
    }

    @Test
    public void testIsNotEmptyFileRecognizedFormat() throws IOException {
        Assert.assertFalse(importer.isRecognizedFormat(emptyFileStream));
    }

    @Test
    public void testImportMultipleEntriesInSingleFile() throws IOException {
        try (InputStream is = MedlinePlainImporter.class
                .getResourceAsStream("MedlinePlainImporterTestMultipleEntries.txt")) {

            List<BibEntry> entries = importer.importEntries(is, new OutputPrinterToNull());
            Assert.assertEquals(7, entries.size());

            BibEntry testEntry = entries.get(0);
            Assert.assertEquals("article", testEntry.getType());
            Assert.assertNull(testEntry.getField("month"));
            Assert.assertEquals("Long, Vicky and Marland, Hilary", testEntry.getField("author"));
            Assert.assertEquals(
                    "From danger and motherhood to health and beauty: health advice for the factory girl in early twentieth-century Britain.",
                    testEntry.getField("title"));

            testEntry = entries.get(1);
            Assert.assertEquals("conference", testEntry.getType());
            Assert.assertEquals("06", testEntry.getField("month"));
            Assert.assertNull(testEntry.getField("author"));
            Assert.assertNull(testEntry.getField("title"));

            testEntry = entries.get(2);
            Assert.assertEquals("book", testEntry.getType());
            Assert.assertEquals(
                    "This is a Testtitle: This title should be appended: This title should also be appended. Another append to the Title? LastTitle",
                    testEntry.getField("title"));

            testEntry = entries.get(3);
            Assert.assertEquals("techreport", testEntry.getType());
            Assert.assertNotNull(testEntry.getField("doi"));

            testEntry = entries.get(4);
            Assert.assertEquals("inproceedings", testEntry.getType());
            Assert.assertEquals("Inproceedings book title", testEntry.getField("booktitle"));

            testEntry = entries.get(5);
            Assert.assertEquals("proceedings", testEntry.getType());

            testEntry = entries.get(6);
            Assert.assertEquals("misc", testEntry.getType());

        }
    }

    @Test
    public void testEmptyFileImport() throws IOException {
        List<BibEntry> emptyEntries = importer.importEntries(emptyFileStream, new OutputPrinterToNull());
        Assert.assertEquals(Collections.emptyList(), emptyEntries);
    }

    @Test
    public void testImportSingleEntriesInSingleFiles() throws IOException {
        List<String> testFiles = Arrays.asList("MedlinePlainImporterTestCompleteEntry",
                "MedlinePlainImporterTestMultiAbstract", "MedlinePlainImporterTestMultiTitle",
                "MedlinePlainImporterTestDOI", "MedlinePlainImporterTestInproceeding");
        for (String testFile : testFiles) {
            String medlineFile = testFile + ".txt";
            String bibtexFile = testFile + ".bib";
            assertImportOfMedlineFileEqualsBibtexFile(medlineFile, bibtexFile);
        }
    }

    private void assertImportOfMedlineFileEqualsBibtexFile(String medlineFile, String bibtexFile) throws IOException {
        try (InputStream is = MedlinePlainImporter.class.getResourceAsStream(medlineFile);
                InputStream nis = MedlinePlainImporter.class.getResourceAsStream(bibtexFile)) {
            List<BibEntry> entries = importer.importEntries(is, new OutputPrinterToNull());
            Assert.assertNotNull(entries);
            Assert.assertEquals(1, entries.size());
            BibtexEntryAssert.assertEquals(nis, entries.get(0));
        }
    }

    @Test
    public void testMultiLineComments() throws IOException {
        try (InputStream stream = streamForString("PMID-22664220" + "\n" + "CON - Comment1" + "\n" + "CIN - Comment2"
                + "\n" + "EIN - Comment3" + "\n" + "EFR - Comment4" + "\n" + "CRI - Comment5" + "\n" + "CRF - Comment6"
                + "\n" + "PRIN- Comment7" + "\n" + "PROF- Comment8" + "\n" + "RPI - Comment9" + "\n" + "RPF - Comment10"
                + "\n" + "RIN - Comment11" + "\n" + "ROF - Comment12" + "\n" + "UIN - Comment13" + "\n"
                + "UOF - Comment14" + "\n" + "SPIN- Comment15" + "\n" + "ORI - Comment16");) {
            List<BibEntry> actualEntries = importer.importEntries(stream, new OutputPrinterToNull());

            BibEntry expectedEntry = new BibEntry();
            expectedEntry.setField("comment",
                    "Comment1" + "\n" + "Comment2" + "\n" + "Comment3" + "\n" + "Comment4" + "\n" + "Comment5" + "\n"
                            + "Comment6" + "\n" + "Comment7" + "\n" + "Comment8" + "\n" + "Comment9" + "\n"
                            + "Comment10" + "\n" + "Comment11" + "\n" + "Comment12" + "\n" + "Comment13" + "\n"
                            + "Comment14" + "\n" + "Comment15" + "\n" + "Comment16");
            BibtexEntryAssert.assertEquals(Arrays.asList(expectedEntry), actualEntries);
        }
    }

    @Test
    public void testKeyWords() throws IOException {
        try (InputStream stream = streamForString("PMID-22664795" + "\n" + "MH  - Female" + "\n" + "OT  - Male");) {
            List<BibEntry> actualEntries = importer.importEntries(stream, new OutputPrinterToNull());

            BibEntry expectedEntry = new BibEntry();
            expectedEntry.setField("keywords", "Female, Male");
            BibtexEntryAssert.assertEquals(Arrays.asList(expectedEntry), actualEntries);
        }
    }

    @Test
    public void testAllArticleTypes() throws IOException {
        try (InputStream stream = streamForString("PMID-22664795" + "\n" + "PT  - journal article" + "\n"
                + "PT  - classical article" + "\n" + "PT  - corrected and republished article" + "\n"
                + "PT  - introductory journal article" + "\n" + "PT  - newspaper article");) {
            List<BibEntry> actualEntries = importer.importEntries(stream, new OutputPrinterToNull());

            BibEntry expectedEntry = new BibEntry();
            expectedEntry.setType("article");
            BibtexEntryAssert.assertEquals(Arrays.asList(expectedEntry), actualEntries);
        }
    }

    @Test
    public void testGetFormatName() {
        Assert.assertEquals("MedlinePlain", importer.getFormatName());
    }

    @Test
    public void testGetCLIId() {
        Assert.assertEquals("medlineplain", importer.getCLIId());
    }

}
