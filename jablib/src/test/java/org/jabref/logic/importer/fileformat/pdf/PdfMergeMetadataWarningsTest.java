package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.StandardField;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class PdfMergeMetadataWarningsTest {

    @TempDir
    Path tempDir;

    private PdfMergeMetadataImporter importer;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences preferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.fieldPreferences().getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());
        when(preferences.grobidPreferences().isGrobidEnabled()).thenReturn(false);
        importer = new PdfMergeMetadataImporter(preferences);
    }

    @Test
    void preservesWarningWhenNoEntriesAreExtracted() throws IOException, ParseException {
        try (PDDocument document = createAmbiguousBook()) {
            ParserResult result = importer.importDatabase(tempDir.resolve("book.pdf"), document);

            assertEquals(List.of(), result.getDatabase().getEntries());
            assertEquals(ambiguousBookWarnings(), result.warnings());
        }
    }

    @Test
    void preservesWarningWhenAnotherImporterProvidesAnEntry() throws IOException, ParseException {
        try (PDDocument document = createAmbiguousBook()) {
            document.getDocumentInformation().setTitle("Book title from PDF metadata");

            ParserResult result = importer.importDatabase(tempDir.resolve("book.pdf"), document);

            assertEquals(1, result.getDatabase().getEntries().size());
            assertEquals(Optional.of("Book title from PDF metadata"), result.getDatabase().getEntries().getFirst().getTitle());
            assertEquals(Optional.empty(), result.getDatabase().getEntries().getFirst().getField(StandardField.ISBN));
            assertEquals(ambiguousBookWarnings(), result.warnings());
        }
    }

    @Test
    void doesNotCarryWarningsIntoSubsequentImports() throws IOException, ParseException {
        try (PDDocument ambiguousBook = createAmbiguousBook();
             PDDocument cleanDocument = new PDDocument()) {
            ParserResult ambiguousResult = importer.importDatabase(tempDir.resolve("ambiguous.pdf"), ambiguousBook);
            assertEquals(ambiguousBookWarnings(), ambiguousResult.warnings());

            cleanDocument.addPage(new PDPage());
            ParserResult emptyResult = importer.importDatabase(tempDir.resolve("empty.pdf"), cleanDocument);
            assertEquals(List.of(), emptyResult.getDatabase().getEntries());
            assertEquals(List.of(), emptyResult.warnings());

            cleanDocument.getDocumentInformation().setTitle("Another book");
            ParserResult populatedResult = importer.importDatabase(tempDir.resolve("clean.pdf"), cleanDocument);
            assertEquals(1, populatedResult.getDatabase().getEntries().size());
            assertEquals(Optional.of("Another book"), populatedResult.getDatabase().getEntries().getFirst().getTitle());
            assertEquals(List.of(), populatedResult.warnings());
        }
    }

    private PDDocument createAmbiguousBook() throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            content.setLeading(16);
            content.newLineAtOffset(50, 700);
            for (String line : List.of("Publisher: Example Press", "All rights reserved",
                    "ISBN 9780596520687", "ISBN 9780134685991")) {
                content.showText(line);
                content.newLine();
            }
            content.endText();
        }

        return document;
    }

    private List<String> ambiguousBookWarnings() {
        return List.of(Localization.lang("More than one ISBN found: Found %0. Not adding.",
                "9780596520687, 9780134685991"));
    }
}
