package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PdfVerbatimBibtexImporterTest {

    private PdfVerbatimBibtexImporter importer;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.fieldPreferences().getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());
        importer = new PdfVerbatimBibtexImporter(importFormatPreferences);
    }

    @Test
    void entryIsFoundWhenPrecededAndFollowedByPageText(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("bibtexWithSurroundingText.pdf");
        writePdf(file, List.of(
                "Applying IoT Patterns to Smart Factory Systems",
                "Jane Doe and John Doe",
                "[lastname]@iaas.uni-stuttgart.de",
                "[firstname].[lastname]@example.com",
                "@inproceedings{Doe2017,",
                "author = {Doe, Jane and Doe, John},",
                "booktitle = {Proceedings of the Advanced Summer School on Service Oriented Computing},",
                "pages = {1--10},",
                "title = {{Applying IoT Patterns to Smart Factory Systems}},",
                "year = {2017}",
                "}",
                "The full version of this publication has been presented as a poster."));

        List<BibEntry> result = importer.importDatabase(file).getDatabase().getEntries();

        BibEntry expected = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("Doe2017")
                .withField(StandardField.AUTHOR, "Doe, Jane and Doe, John")
                .withField(StandardField.BOOKTITLE, "Proceedings of the Advanced Summer School on Service Oriented Computing")
                .withField(StandardField.PAGES, "1--10")
                .withField(StandardField.TITLE, "{Applying IoT Patterns to Smart Factory Systems}")
                .withField(StandardField.YEAR, "2017");

        assertEquals(List.of(expected), result);
    }

    @Test
    void pageWithoutBibtexReturnsNoEntries(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("noBibtex.pdf");
        writePdf(file, List.of(
                "Applying IoT Patterns to Smart Factory Systems",
                "Jane Doe and John Doe",
                "[lastname]@iaas.uni-stuttgart.de"));

        List<BibEntry> result = importer.importDatabase(file).getDatabase().getEntries();

        assertEquals(List.of(), result);
    }

    private static void writePdf(Path file, List<String> lines) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.setLeading(14);
                contentStream.newLineAtOffset(25, 750);
                for (String line : lines) {
                    contentStream.showText(line);
                    contentStream.newLine();
                }
                contentStream.endText();
            }
            document.save(file.toFile());
        }
    }

    @Test
    void doesNotHandleEncryptedPdfs() throws URISyntaxException {
        Path file = Path.of(PdfVerbatimBibtexImporter.class.getResource("/pdfs/encrypted.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file).getDatabase().getEntries();
        assertEquals(List.of(), result);
    }

    @Test
    void importTwiceWorksAsExpected() throws URISyntaxException {
        Path file = Path.of(PdfVerbatimBibtexImporterTest.class.getResource("mixedMetadata.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file).getDatabase().getEntries();

        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("jabreftest2021")
                .withField(StandardField.AUTHOR, "Me, myself and I")
                .withField(StandardField.TITLE, "Something")
                .withField(StandardField.VOLUME, "1")
                .withField(StandardField.JOURNAL, "Some Journal")
                .withField(StandardField.YEAR, "2021")
                .withField(StandardField.ISBN, "0134685997");

        List<BibEntry> resultSecondImport = importer.importDatabase(file).getDatabase().getEntries();

        assertEquals(List.of(expected), result);
        assertEquals(List.of(expected), resultSecondImport);
    }
}
