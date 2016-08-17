package net.sf.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(MockitoJUnitRunner.class)
public class BibTeXMLImporterTest {

    private static final String FILEFORMAT_PATH = "src/test/resources/net/sf/jabref/logic/importer/fileformat";
    private BibTeXMLImporter importer;


    /**
     * Generates a List of all files in the package "/src/test/resources/net/sf/jabref/logic/importer/fileformat"
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
    public void testGetFormatName() {
        assertEquals("BibTeXML", importer.getFormatName());
    }

    @Test
    public void testGetCLIId() {
        assertEquals("bibtexml", importer.getId());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileExtensions.BIBTEXML, importer.getExtensions());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Importer for the BibTeXML format.", importer.getDescription());
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException {
        List<Path> list = getTestFiles().stream()
                .filter(n -> !n.getFileName().toString().startsWith("BibTeXMLImporterTest"))
                .collect(Collectors.toList());

        for (Path file : list) {
            assertFalse(file.toString(), importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }
}
