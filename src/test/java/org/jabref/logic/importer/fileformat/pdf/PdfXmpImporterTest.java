package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PdfXmpImporterTest {

    private PdfXmpImporter importer;

    @BeforeEach
    void setUp() {
        importer = new PdfXmpImporter(mock(XmpPreferences.class));
    }

    @Disabled("XMP reader prints warnings to the logger when parsing does not work")
    @Test
    void importEncryptedFileReturnsError() throws URISyntaxException {
        Path file = Path.of(PdfXmpImporterTest.class.getResource("/pdfs/encrypted.pdf").toURI());
        ParserResult result = importer.importDatabase(file);
        assertTrue(result.hasWarnings());
    }

    @Test
    void importEntries() throws URISyntaxException {
        Path file = Path.of(PdfXmpImporterTest.class.getResource("annotated.pdf").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();

        BibEntry expected = new BibEntry()
                .withField(StandardField.ABSTRACT, "how to annotate a pdf")
                .withField(StandardField.AUTHOR, "Chris")
                .withField(StandardField.KEYWORDS, "pdf, annotation")
                .withField(StandardField.FILE, ":" + file.toString().replace("\\", "/").replace(":", "\\:") + ":PDF")
                .withField(StandardField.TITLE, "The best Pdf ever");

        assertEquals(List.of(expected), bibEntries);
    }

    @Test
    void pdf2024SPLCBecker() throws URISyntaxException {
        Path file = Path.of(PdfXmpImporterTest.class.getResource("2024_SPLC_Becker.pdf").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();

        BibEntry expected = new BibEntry()
                .withField(StandardField.TITLE, "Not Quite There Yet: Remaining Challenges in Systems and Software Product Line Engineering as Perceived by Industry Practitioners")
                .withField(StandardField.ABSTRACT, "-  Software and its engineering  ->  Software product lines.")
                .withField(StandardField.DOI, "10.1145/3646548.3672587")
                .withField(StandardField.FILE, ":" + file.toString().replace("\\", "/").replace(":", "\\:") + ":PDF");

        assertEquals(List.of(expected), bibEntries);
    }

    @Test
    void isRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Path.of(PdfXmpImporterTest.class.getResource("annotated.pdf").toURI());
        assertTrue(importer.isRecognizedFormat(file));
    }
}
