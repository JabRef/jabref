package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BibTeXMLImporterTest {

    private final String FILEFORMAT_PATH = "src/test/resources/net/sf/jabref/importer/fileformat";


    /**
     * Generates a List of all files in the package "/src/test/resources/net/sf/jabref/importer/fileformat"
     * @return A list of Names
     * @throws IOException
     */
    public List<String> getTestFiles() throws IOException {
        List<String> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(FILEFORMAT_PATH))) {
            stream.forEach(n -> files.add(n.getFileName().toString()));
        }
        return files;

    }

    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testExceptionOnInputStream() throws IOException {
        try (InputStream is = Mockito.mock(InputStream.class)) {
            Mockito.doThrow(new IOException()).when(is).read();

            BibTeXMLImporter importer = new BibTeXMLImporter();
            List<BibEntry> entry = importer.importEntries(is, new OutputPrinterToNull());
            Assert.assertTrue(entry.isEmpty());
        }
    }

    @Test
    public void testGetItemsEmpty() {
        BibTeXMLHandler handler = new BibTeXMLHandler();
        Assert.assertEquals(Collections.emptyList(), handler.getItems());
    }

    @Test
    public void testGetFormatName() {
        BibTeXMLImporter importer = new BibTeXMLImporter();
        Assert.assertEquals("BibTeXML", importer.getFormatName());
    }

    @Test
    public void testGetCLIId() {
        BibTeXMLImporter importer = new BibTeXMLImporter();
        Assert.assertEquals("bibtexml", importer.getCLIId());
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException {
        BibTeXMLImporter importer = new BibTeXMLImporter();

        List<String> list = getTestFiles().stream().filter(n -> !n.startsWith("BibTeXMLImporterTest"))
                .collect(Collectors.toList());

        for (String str : list) {
            try (InputStream is = BibTeXMLImporter.class.getResourceAsStream(str)) {
                Assert.assertFalse(importer.isRecognizedFormat(is));
            }
        }
    }
}
