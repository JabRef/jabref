package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests for [PdfMergeMetadataImporter] that work without network access, complementing
/// [PdfMergeMetadataImporterTest] (which is tagged as fetcher test and thus not run by default).
///
/// The cross-check logic under test lives in [PdfContentImporter], but is exercised here through the
/// merge importer, which is where it takes effect.
class PdfMergeMetadataImporterOfflineTest {

    private static final String DOCUMENT_TEXT = """
            An Approach to Automatically Check the Compliance of Declarative Deployment Models
            Christoph Krieger, Uwe Breitenbücher, Kálmán Képes, and Frank Leymann
            Institute of Architecture of Application Systems, University of Stuttgart
            Abstract. The automation of application deployment has evolved into an important issue.
            """;

    /// Single creator-style candidate (as produced from PDF document properties): a Misc entry without a
    /// citation key. Its author survives only when confirmed by the document text; an empty expected value
    /// means it is dropped. Columns: author | leadingPagesText | expectedAuthor (an empty leadingPagesText
    /// column arrives as `null`, modelling a PDF whose text could not be extracted).
    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            Krieger, Christoph        | Christoph Krieger, Uwe Breitenbücher, University of Stuttgart | Krieger, Christoph
            Richter, Markus           | Christoph Krieger, Uwe Breitenbücher, University of Stuttgart |
            Li, Xin                   | Christoph Krieger, Uwe Breitenbücher, University of Stuttgart |
            Doe, John and Smith, Jane | Christoph Krieger, Uwe Breitenbücher, University of Stuttgart | Doe, John and Smith, Jane
            Richter, Markus           |                                                              | Richter, Markus
            """)
    void singleCreatorCandidateAuthorIsCrossCheckedAgainstText(String author, String leadingPagesText, String expectedAuthor) {
        BibEntry candidate = new BibEntry().withField(StandardField.AUTHOR, author);

        BibEntry merged = PdfMergeMetadataImporter.mergeCandidates(List.of(candidate), leadingPagesText);

        assertEquals(Optional.ofNullable(expectedAuthor), merged.getField(StandardField.AUTHOR));
    }

    @Test
    void authorAbsentFromDocumentTextIsReplacedByConfirmedCandidate() {
        BibEntry documentInformation = new BibEntry()
                .withField(StandardField.AUTHOR, "Richter, Markus");
        BibEntry content = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "Krieger, Christoph and Breitenbücher, Uwe and Képes, Kálmán and Leymann, Frank");

        BibEntry merged = PdfMergeMetadataImporter.mergeCandidates(List.of(documentInformation, content), DOCUMENT_TEXT);

        assertEquals(Optional.of("Krieger, Christoph and Breitenbücher, Uwe and Képes, Kálmán and Leymann, Frank"),
                merged.getField(StandardField.AUTHOR));
    }

    @Test
    void authorConfirmedDespiteDiacriticsAndHyphenationDifferences() {
        BibEntry documentInformation = new BibEntry()
                .withField(StandardField.AUTHOR, "Breitenbücher, Uwe");
        String hyphenatedText = "A model checking approach developed by Uwe Breitenbü-\ncher in Stuttgart.";

        BibEntry merged = PdfMergeMetadataImporter.mergeCandidates(List.of(documentInformation), hyphenatedText);

        assertEquals(Optional.of("Breitenbücher, Uwe"), merged.getField(StandardField.AUTHOR));
    }

    @Test
    void authorConfirmedByRawTokenWhenNameParsingFails() {
        // Separator characters from broken XMP decoding keep AuthorList from finding proper family names;
        // the raw words of the value still occur in the text and must count as confirmation
        BibEntry xmpMetadata = new BibEntry()
                .withField(StandardField.AUTHOR, "Filippo RiccaⰠ﻿Alessandro MarchettoⰠ﻿Andrea StoccoⰠ");

        BibEntry merged = PdfMergeMetadataImporter.mergeCandidates(List.of(xmpMetadata),
                "A grey literature review by Filippo Ricca, Alessandro Marchetto, and Andrea Stocco.");

        assertEquals(Optional.of("Filippo RiccaⰠ﻿Alessandro MarchettoⰠ﻿Andrea StoccoⰠ"),
                merged.getField(StandardField.AUTHOR));
    }

    @Test
    void unconfirmedAuthorFromEntryWithCitationKeyIsKept() {
        BibEntry jabRefWrittenMetadata = new BibEntry()
                .withCitationKey("Doe2020")
                .withField(StandardField.AUTHOR, "Doe, John");

        BibEntry merged = PdfMergeMetadataImporter.mergeCandidates(List.of(jabRefWrittenMetadata), DOCUMENT_TEXT);

        assertEquals(Optional.of("Doe, John"), merged.getField(StandardField.AUTHOR));
    }

    @Test
    void unconfirmedAuthorFromEntryWithExplicitTypeIsKept() {
        BibEntry jabRefWrittenMetadata = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Doe, John");

        BibEntry merged = PdfMergeMetadataImporter.mergeCandidates(List.of(jabRefWrittenMetadata), DOCUMENT_TEXT);

        assertEquals(Optional.of("Doe, John"), merged.getField(StandardField.AUTHOR));
    }

    @Test
    void wordProducedPdfDoesNotGetCreatorAccountAsAuthor(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("word-export.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                contentStream.newLineAtOffset(72, 750);
                contentStream.showText("Compliance Checking of Deployment Models");
                contentStream.endText();
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(72, 700);
                contentStream.showText("Abstract. This work checks deployment models automatically.");
                contentStream.endText();
            }
            // Office suites store the OS account of the person converting the document here
            document.getDocumentInformation().setAuthor("Markus Richter");
            document.save(file.toFile());
        }

        GrobidPreferences grobidPreferences = mock(GrobidPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(grobidPreferences.isGrobidEnabled()).thenReturn(false);
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.fieldPreferences().getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());
        when(importFormatPreferences.grobidPreferences()).thenReturn(grobidPreferences);
        PdfMergeMetadataImporter importer = new PdfMergeMetadataImporter(importFormatPreferences);

        List<BibEntry> result = importer.importDatabase(file).getDatabase().getEntries();

        assertEquals(Optional.empty(), result.getFirst().getField(StandardField.AUTHOR));
    }
}
