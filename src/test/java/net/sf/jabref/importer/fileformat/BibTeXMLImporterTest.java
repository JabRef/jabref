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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BibTeXMLImporterTest {

    private final String FILEFORMAT_PATH = "src/test/resources/net/sf/jabref/importer/fileformat";


    /**
     * Generates a List of all files in the package "/src/test/resources/net/sf/jabref/importer/fileformat"
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

    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
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
        Assert.assertEquals("bibtexml", importer.getId());
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException {
        BibTeXMLImporter importer = new BibTeXMLImporter();

        List<Path> list = getTestFiles().stream().filter(n -> !n.getFileName().toString().startsWith("BibTeXMLImporterTest"))
                .collect(Collectors.toList());

        for (Path file : list) {
            Assert.assertFalse(file.toString(), importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }
}
