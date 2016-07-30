package net.sf.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MsBibImporterTest {

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testsGetExtensions() {
        MsBibImporter importer = new MsBibImporter();
        List<String> extensions = new ArrayList<>();
        extensions.add(".xml");

        Assert.assertEquals(extensions.get(0), importer.getExtensions().get(0));
    }

    @Test
    public void testGetDescription() {
        MsBibImporter importer = new MsBibImporter();
        Assert.assertEquals("Importer for the MS Office 2007 XML bibliography format.", importer.getDescription());
    }

    @Test
    public final void testIsNotRecognizedFormat() throws Exception {
        MsBibImporter testImporter = new MsBibImporter();
        List<String> notAccepted = Arrays.asList("CopacImporterTest1.txt", "IsiImporterTest1.isi",
                "IsiImporterTestInspec.isi", "emptyFile.xml", "IsiImporterTestWOS.isi");
        for (String s : notAccepted) {
            Path file = Paths.get(MsBibImporter.class.getResource(s).toURI());
            Assert.assertFalse(testImporter.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

    @Test
    public final void testImportEntriesEmpty() throws IOException, URISyntaxException {
        MsBibImporter testImporter = new MsBibImporter();
        Path file = Paths.get(MsBibImporter.class.getResource("MsBibImporterTest.xml").toURI());
        List<BibEntry> entries = testImporter.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();
        Assert.assertEquals(Collections.emptyList(), entries);
    }

    @Test
    public final void testImportEntriesNotRecognizedFormat() throws IOException, URISyntaxException {
        MsBibImporter testImporter = new MsBibImporter();
        Path file = Paths.get(MsBibImporter.class.getResource("CopacImporterTest1.txt").toURI());
        List<BibEntry> entries = testImporter.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();
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
        Assert.assertEquals("msbib", testImporter.getId());
    }

}
