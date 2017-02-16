package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.util.FileExtensions;

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

    /**
     * Generates a List of all files in the package "/src/test/resources/org/jabref/logic/importer/fileformat"
     * @return A list of Names
     * @throws IOException
     */
    public List<Path> getTestFiles() throws Exception {
        try (Stream<Path> stream = Files.list(Paths.get(MedlineImporterTest.class.getResource("").toURI()))) {
            return stream.filter(p -> !Files.isDirectory(p)).collect(Collectors.toList());
        }
    }

    @Before
    public void setUp() throws Exception {
        this.importer = new MedlineImporter();
    }

    @Test
    public void testGetFormatName() {
        assertEquals("Medline/PubMed", importer.getName());
    }

    @Test
    public void testGetCLIId() {
        assertEquals("medline", importer.getId());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileExtensions.MEDLINE, importer.getExtensions());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Importer for the Medline format.", importer.getDescription());
    }

    @Test
    public void testIsRecognizedFormatReject() throws Exception {
        List<Path> list = getTestFiles().stream().filter(n -> !n.getFileName().toString().startsWith("MedlineImporter"))
                .collect(Collectors.toList());

        for (Path file : list) {
            Assert.assertFalse(file.toString(), importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }
}
