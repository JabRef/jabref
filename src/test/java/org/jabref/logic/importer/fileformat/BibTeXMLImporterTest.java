package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.util.FileExtensions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(MockitoJUnitRunner.class)
public class BibTeXMLImporterTest {

    private BibTeXMLImporter importer;


    /**
     * Generates a List of all files in the package "/src/test/resources/org/jabref/logic/importer/fileformat"
     *
     * @return A list of Names
     * @throws IOException
     */
    public List<Path> getTestFiles() throws Exception {
        try (Stream<Path> stream = Files.list(Paths.get(BibTeXMLImporterTest.class.getResource("").toURI()))) {
            return stream.filter(p -> !Files.isDirectory(p)).collect(Collectors.toList());
        }

    }

    @Before
    public void setUp() throws Exception {
        importer = new BibTeXMLImporter();
    }

    @Test
    public void testGetFormatName() {
        assertEquals("BibTeXML", importer.getName());
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
    public void testIsRecognizedFormatReject() throws Exception {
        List<Path> list = getTestFiles().stream()
                .filter(n -> !n.getFileName().toString().startsWith("BibTeXMLImporterTest"))
                .collect(Collectors.toList());

        for (Path file : list) {
            assertFalse(file.toString(), importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
        }
    }
}
