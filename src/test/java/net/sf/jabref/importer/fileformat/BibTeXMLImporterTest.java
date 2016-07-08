package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.nio.charset.Charset;
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BibTeXMLImporterTest {

    private final String FILEFORMAT_PATH = "src/test/resources/net/sf/jabref/importer/fileformat";
    private BibTeXMLImporter importer;

    /**
     * Generates a List of all files in the package "/src/test/resources/net/sf/jabref/importer/fileformat"
     *
     * @return A list of Names
     * @throws IOException
     */
    public List<Path> getTestFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(FILEFORMAT_PATH))) {
            stream.forEach(files::add);
        }
        return files;

    }

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        importer = new BibTeXMLImporter();
    }

    @Test
    public void testGetItemsEmpty() {
        BibTeXMLHandler handler = new BibTeXMLHandler();
        Assert.assertEquals(Collections.emptyList(), handler.getItems());
    }

    @Test
    public void testGetFormatName() {
        Assert.assertEquals("BibTeXML", importer.getFormatName());
    }

    @Test
    public void testGetCLIId() {
        Assert.assertEquals("bibtexml", importer.getId());
    }

    @Test
    public void testsGetExtensions() {
        Assert.assertEquals(Collections.singletonList(".xml"), importer.getExtensions());
    }

    @Test
    public void testGetDescription() {
        Assert.assertEquals("Importer for the BibTeXML format.", importer.getDescription());
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException {
        List<Path> list = getTestFiles().stream()
                .filter(n -> !n.getFileName().toString().startsWith("BibTeXMLImporterTest"))
                .collect(Collectors.toList());

        for (Path file : list) {
            Assert.assertFalse(file.toString(), importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }
}
