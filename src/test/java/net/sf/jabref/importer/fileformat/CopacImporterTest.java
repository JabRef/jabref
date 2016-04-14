package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CopacImporterTest {

    @Before
    public void setUp() throws Exception {
        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
        }
    }

    @Test
    public void testIsRecognizedFormat() throws IOException, URISyntaxException {
        CopacImporter importer = new CopacImporter();
        List<String> list = Arrays.asList("CopacImporterTest1.txt", "CopacImporterTest2.txt");
        for (String str : list) {
            Path file = Paths.get(CopacImporterTest.class.getResource(str).toURI());
            Assert.assertTrue(importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

    @Test
    public void testIsNotRecognizedFormat() throws IOException, URISyntaxException {
        CopacImporter importer = new CopacImporter();
        List<String> list = Arrays.asList("IsiImporterTest1.isi", "IsiImporterTestInspec.isi", "IsiImporterTestWOS.isi",
                "IsiImporterTestMedline.isi");
        for (String str : list) {
            Path file = Paths.get(CopacImporterTest.class.getResource(str).toURI());
            Assert.assertFalse(importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

    @Test
    public void testImportEntries() throws IOException, URISyntaxException {
        Globals.prefs.put("defaultEncoding", StandardCharsets.UTF_8.name());

        CopacImporter importer = new CopacImporter();

        Path file = Paths.get(CopacImporterTest.class.getResource("CopacImporterTest1.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();
        Assert.assertEquals(1, entries.size());
        BibEntry entry = entries.get(0);

        Assert.assertEquals("The SIS project : software reuse with a natural language approach", entry.getField("title"));
        Assert.assertEquals(
"Prechelt, Lutz and Universität Karlsruhe. Fakultät für Informatik",
                entry.getField("author"));
        Assert.assertEquals("Interner Bericht ; Nr.2/92", entry.getField("series"));
        Assert.assertEquals("1992", entry.getField("year"));
        Assert.assertEquals("Karlsruhe :  Universitat Karlsruhe, Fakultat fur Informatik", entry.getField("publisher"));
        Assert.assertEquals("book", entry.getType());
    }

    @Test
    public void testImportEntries2() throws IOException, URISyntaxException {
        CopacImporter importer = new CopacImporter();

        Path file = Paths.get(CopacImporterTest.class.getResource("CopacImporterTest2.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();
        Assert.assertEquals(2, entries.size());
        BibEntry one = entries.get(0);

        Assert.assertEquals("Computing and operational research at the London Hospital", one.getField("title"));

        BibEntry two = entries.get(1);

        Assert.assertEquals("Real time systems : management and design", two.getField("title"));
    }
}
