package net.sf.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MsBibImporterTest {

    @Test
    public void testsGetExtensions() {
        MsBibImporter importer = new MsBibImporter();
        assertEquals(FileExtensions.MSBIB, importer.getExtensions());
    }

    @Test
    public void testGetDescription() {
        MsBibImporter importer = new MsBibImporter();
        assertEquals("Importer for the MS Office 2007 XML bibliography format.", importer.getDescription());
    }

    @Test
    public final void testIsNotRecognizedFormat() throws Exception {
        MsBibImporter testImporter = new MsBibImporter();
        List<String> notAccepted = Arrays.asList("CopacImporterTest1.txt", "IsiImporterTest1.isi",
                "IsiImporterTestInspec.isi", "emptyFile.xml", "IsiImporterTestWOS.isi");
        for (String s : notAccepted) {
            Path file = Paths.get(MsBibImporter.class.getResource(s).toURI());
            assertFalse(testImporter.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

    @Test
    public final void testImportEntriesEmpty() throws IOException, URISyntaxException {
        MsBibImporter testImporter = new MsBibImporter();
        Path file = Paths.get(MsBibImporter.class.getResource("EmptyMsBib_Test.xml").toURI());
        List<BibEntry> entries = testImporter.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();
        assertEquals(Collections.emptyList(), entries);
    }

    @Test
    public final void testImportEntriesNotRecognizedFormat() throws IOException, URISyntaxException {
        MsBibImporter testImporter = new MsBibImporter();
        Path file = Paths.get(MsBibImporter.class.getResource("CopacImporterTest1.txt").toURI());
        List<BibEntry> entries = testImporter.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();
        assertEquals(0, entries.size());
    }

    @Test
    public final void testGetFormatName() {
        MsBibImporter testImporter = new MsBibImporter();
        assertEquals("MSBib", testImporter.getName());
    }

    @Test
    public final void testGetCommandLineId() {
        MsBibImporter testImporter = new MsBibImporter();
        assertEquals("msbib", testImporter.getId());
    }

}
