package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class PdfXmpImporterTest {

    private PdfXmpImporter importer;

    private static Stream<String> invalidFileNames() throws IOException {
        Predicate<String> fileName = name -> !name.contains("annotated.pdf");
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @BeforeEach
    public void setUp() {
        importer = new PdfXmpImporter(mock(XmpPreferences.class));
    }

    @Test
    public void getFormatName() {
        assertEquals("XMP-annotated PDF", importer.getName());
    }

    @Test
    public void sGetExtensions() {
        assertEquals(StandardFileType.PDF, importer.getFileType());
    }

    @Test
    public void getDescription() {
        assertEquals("Wraps the XMPUtility function to be used as an Importer.", importer.getDescription());
    }

    @Disabled("XMP reader prints warnings to the logger when parsing does not work")
    @Test
    public void importEncryptedFileReturnsError() throws URISyntaxException {
        Path file = Path.of(PdfXmpImporterTest.class.getResource("/pdfs/encrypted.pdf").toURI());
        ParserResult result = importer.importDatabase(file);
        assertTrue(result.hasWarnings());
    }

    @Test
    public void importEntries() throws URISyntaxException {
        Path file = Path.of(PdfXmpImporterTest.class.getResource("annotated.pdf").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();

        assertEquals(1, bibEntries.size());

        BibEntry be0 = bibEntries.getFirst();
        assertEquals(Optional.of("how to annotate a pdf"), be0.getField(StandardField.ABSTRACT));
        assertEquals(Optional.of("Chris"), be0.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("pdf, annotation"), be0.getField(StandardField.KEYWORDS));
        assertEquals(Optional.of("The best Pdf ever"), be0.getField(StandardField.TITLE));
    }

    @Test
    public void isRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Path.of(PdfXmpImporterTest.class.getResource("annotated.pdf").toURI());
        assertTrue(importer.isRecognizedFormat(file));
    }

    @ParameterizedTest
    @MethodSource("invalidFileNames")
    public void isRecognizedFormatReject(String fileName) throws IOException, URISyntaxException {
        ImporterTestEngine.testIsNotRecognizedFormat(importer, fileName);
    }

    @Test
    public void getCommandLineId() {
        assertEquals("xmp", importer.getId());
    }
}
