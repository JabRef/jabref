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
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

/**
 * Articles in the medline format can be downloaded from http://www.ncbi.nlm.nih.gov/pubmed/.
 * 1. Search for a term and make sure you have selected the PubMed database
 * 2. Select the results you want to export by checking their checkboxes
 * 3. Press on the 'Send to' drop down menu on top of the search results
 * 4. Select 'File' as Destination and 'XML' as Format
 * 5. Press 'Create File' to download your search results in a medline xml file
 *
 * @author Daniel Mair/Bruehl
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class MedlineImporterTest {

    private MedlineImporter importer;
    private static final String FILEFORMAT_PATH = "src/test/resources/net/sf/jabref/importer/fileformat";

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

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        this.importer = new MedlineImporter();
    }

    @Test
    public void testGetItemsEmpty() {
        MedlineHandler handler = new MedlineHandler();
        assertEquals(Collections.emptyList(), handler.getItems());
    }

    @Test
    public void testGetFormatName() {
        assertEquals("Medline", importer.getFormatName());
    }

    @Test
    public void testGetCLIId() {
        assertEquals("medline", importer.getId());
    }

    @Test
    public void testsGetExtensions() {
        List<String> extensions = new ArrayList<>();
        extensions.add(".nbib");
        extensions.add(".xml");

        assertEquals(extensions.get(0), importer.getExtensions().get(0));
        assertEquals(extensions.get(1), importer.getExtensions().get(1));
    }

    @Test
    public void testGetDescription() {
        assertEquals("Importer for the Medline format.", importer.getDescription());
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException {
       List<Path> list = getTestFiles().stream().filter(n -> !n.getFileName().toString().startsWith("MedlineImporter"))
                .collect(Collectors.toList());

        for (Path file : list) {
            Assert.assertFalse(file.toString(), importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }
}
