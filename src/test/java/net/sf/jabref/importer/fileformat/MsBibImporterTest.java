package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;

public class MsBibImporterTest {

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public final void testIsNotRecognizedFormat() throws Exception {
        MsBibImporter testImporter = new MsBibImporter();
        List<String> notAccepted = Arrays.asList("CopacImporterTest1.txt", "IsiImporterTest1.isi",
                "IsiImporterTestInspec.isi", "emptyFile.xml", "IsiImporterTestWOS.isi");
        for (String s : notAccepted) {
            try (InputStream stream = MsBibImporter.class.getResourceAsStream(s)) {
                Assert.assertFalse(testImporter.isRecognizedFormat(stream));
            }
        }

    }

    @Test
    public final void testImportEntriesEmpty() throws IOException {
        MsBibImporter testImporter = new MsBibImporter();

        List<BibEntry> entries = testImporter.importEntries(
                MsBibImporterTest.class.getResourceAsStream("MsBibImporterTest.xml"), new OutputPrinterToNull());
        Assert.assertEquals(Collections.emptyList(), entries);
    }

    @Test
    public final void testImportEntriesNotRecognizedFormat() throws IOException {
        MsBibImporter testImporter = new MsBibImporter();

        List<BibEntry> entries = testImporter.importEntries(
                MsBibImporterTest.class.getResourceAsStream("CopacImporterTest1.txt"), new OutputPrinterToNull());
        Assert.assertEquals(0, entries.size());
    }

    @Test
    public final void testGetFormatName() {
        MsBibImporter testImporter = new MsBibImporter();
        Assert.assertEquals("MSBib", testImporter.getFormatName());
    }

    @Test
    public final void testGetCommandLineId() {
        MsBibImporter testImporter = new MsBibImporter();
        Assert.assertEquals("msbib", testImporter.getCommandLineId());
    }

}
