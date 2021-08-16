package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CffImporterTest {

    private CffImporter importer;

    @BeforeEach
    public void setUp() {
        importer = new CffImporter();
    }

    @Test
    public void testGetFormatName() {
        assertEquals("CFF", importer.getName());
    }

    @Test
    public void testGetCLIId() {
        assertEquals("cff", importer.getId());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(StandardFileType.CFF, importer.getFileType());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Importer for the CFF format. Is only used to cite software, one "
                + "entry per file.", importer.getDescription());
    }

    @Test
    public void testIsRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestValid.cff").toURI());
        assertTrue(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("CffImporterTestInvalid1.cff", "CffImporterTestInvalid2.cff");

        for (String string : list) {
            Path file = Path.of(CffImporterTest.class.getResource(string).toURI());
            assertFalse(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testImportEntries() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestValid.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        BibEntry entry = bibEntries.get(0);

        assertEquals(entry.getField(StandardField.AUTHOR), Optional.of("Joe van Smith"));
        assertEquals(entry.getField(StandardField.TITLE), Optional.of("Test"));
        assertEquals(entry.getField(StandardField.URL), Optional.of("www.google.com"));
        assertEquals(entry.getField(StandardField.REPOSITORY), Optional.of("www.github.com"));
        assertEquals(entry.getField(StandardField.DOI), Optional.of("10.0000/TEST"));
        assertEquals(entry.getField(StandardField.DATE), Optional.of("2000-07-02"));
        assertEquals(entry.getField(StandardField.COMMENT), Optional.of("Test entry."));
        assertEquals(entry.getField(StandardField.ABSTRACT), Optional.of("Test abstract."));
        assertEquals(entry.getField(StandardField.LICENSE), Optional.of("MIT"));
        assertEquals(entry.getField(StandardField.VERSION), Optional.of("1.0"));

    }
}
