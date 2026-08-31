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
            A Study of Placeholder Metadata in Automated Testing
            Alice Doe, Bob Smith, Carol Roe, and Dave Poe
            Institute of Placeholder Studies, Example University
            Abstract. This text exists only to exercise the author cross-check.
            """;

    /// Single creator-style candidate (as produced from PDF document properties): a Misc entry without a
    /// citation key. Its author survives only when confirmed by the document text; an empty expected value
    /// means it is dropped. Columns: author | leadingPagesText | expectedAuthor (an empty leadingPagesText
    /// column arrives as `null`, modelling a PDF whose text could not be extracted).
    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            Doe, Alice                  | Alice Doe, Bob Smith, Example University Testing Lab | Doe, Alice
            Void, Eve                   | Alice Doe, Bob Smith, Example University Testing Lab |
            Test, Carol                 | Alice Doe, Bob Smith, Example University Testing Lab |
            Void, Eve and Null, Mallory | Alice Doe, Bob Smith, Example University Testing Lab | Void, Eve and Null, Mallory
            Void, Eve                   |                                                     | Void, Eve
            """)
    void singleCreatorCandidateAuthorIsCrossCheckedAgainstText(String author, String leadingPagesText, String expectedAuthor) {
        BibEntry candidate = new BibEntry().withField(StandardField.AUTHOR, author);

        BibEntry merged = PdfMergeMetadataImporter.mergeCandidates(List.of(candidate), leadingPagesText);

        assertEquals(Optional.ofNullable(expectedAuthor), merged.getField(StandardField.AUTHOR));
    }

    @Test
    void authorAbsentFromDocumentTextIsReplacedByConfirmedCandidate() {
        BibEntry documentInformation = new BibEntry()
                .withField(StandardField.AUTHOR, "Void, Eve");
        BibEntry content = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "Doe, Alice and Smith, Bob and Roe, Carol and Poe, Dave");

        BibEntry merged = PdfMergeMetadataImporter.mergeCandidates(List.of(documentInformation, content), DOCUMENT_TEXT);

        assertEquals(Optional.of("Doe, Alice and Smith, Bob and Roe, Carol and Poe, Dave"),
                merged.getField(StandardField.AUTHOR));
    }

    @Test
    void authorConfirmedDespiteDiacriticsAndHyphenationDifferences() {
        BibEntry documentInformation = new BibEntry()
                .withField(StandardField.AUTHOR, "Fünkel, Bob");
        String hyphenatedText = "A placeholder method developed by Bob Fün-\nkel in the lab.";

        BibEntry merged = PdfMergeMetadataImporter.mergeCandidates(List.of(documentInformation), hyphenatedText);

        assertEquals(Optional.of("Fünkel, Bob"), merged.getField(StandardField.AUTHOR));
    }

    @Test
    void authorConfirmedByRawTokenWhenNameParsingFails() {
        // Separator characters from broken XMP decoding keep AuthorList from finding proper family names;
        // the raw words of the value still occur in the text and must count as confirmation
        BibEntry xmpMetadata = new BibEntry()
                .withField(StandardField.AUTHOR, "Alice DoeⰠ﻿Bob SmithⰠ﻿Carol RoeⰠ");

        BibEntry merged = PdfMergeMetadataImporter.mergeCandidates(List.of(xmpMetadata),
                "A grey literature review by Alice Doe, Bob Smith, and Carol Roe.");

        assertEquals(Optional.of("Alice DoeⰠ﻿Bob SmithⰠ﻿Carol RoeⰠ"),
                merged.getField(StandardField.AUTHOR));
    }

    @Test
    void unconfirmedAuthorFromEntryWithCitationKeyIsKept() {
        BibEntry jabRefWrittenMetadata = new BibEntry()
                .withCitationKey("Void2020")
                .withField(StandardField.AUTHOR, "Void, Eve");

        BibEntry merged = PdfMergeMetadataImporter.mergeCandidates(List.of(jabRefWrittenMetadata), DOCUMENT_TEXT);

        assertEquals(Optional.of("Void, Eve"), merged.getField(StandardField.AUTHOR));
    }

    @Test
    void unconfirmedAuthorFromEntryWithExplicitTypeIsKept() {
        BibEntry jabRefWrittenMetadata = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Void, Eve");

        BibEntry merged = PdfMergeMetadataImporter.mergeCandidates(List.of(jabRefWrittenMetadata), DOCUMENT_TEXT);

        assertEquals(Optional.of("Void, Eve"), merged.getField(StandardField.AUTHOR));
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
                contentStream.showText("A Placeholder Study of Example Systems");
                contentStream.endText();
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(72, 700);
                contentStream.showText("Abstract. This placeholder text contains no author names.");
                contentStream.endText();
            }
            // Office suites store the OS account of the person converting the document here
            document.getDocumentInformation().setAuthor("Eve Void");
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
