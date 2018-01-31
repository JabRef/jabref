package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.util.FileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class BibTeXMLImporterTest {

    private BibTeXMLImporter importer;

    @BeforeEach
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
        assertEquals(FileType.BIBTEXML, importer.getFileType());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Importer for the BibTeXML format.", importer.getDescription());
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException, URISyntaxException {
        try (Stream<Path> stream = Files.list(Paths.get(BibTeXMLImporterTest.class.getResource("").toURI()))) {
            List<Path> list = stream.filter(p -> !Files.isDirectory(p))
                    .filter(n -> !n.getFileName().toString().startsWith("BibTeXMLImporterTest"))
                    .collect(Collectors.toList());

            for (Path file : list) {
                assertFalse(importer.isRecognizedFormat(file, StandardCharsets.UTF_8), file.toString());
            }
        }
    }
}
