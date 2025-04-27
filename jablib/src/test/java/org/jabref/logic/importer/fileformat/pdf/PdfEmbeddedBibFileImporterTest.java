package org.jabref.logic.importer.fileformat.pdf;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PdfEmbeddedBibFileImporterTest {

    private PdfEmbeddedBibFileImporter importer;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.fieldPreferences().getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());
        importer = new PdfEmbeddedBibFileImporter(importFormatPreferences);
    }

    @Test
    void doesNotHandleEncryptedPdfs() throws URISyntaxException {
        Path file = Path.of(PdfEmbeddedBibFileImporter.class.getResource("/pdfs/encrypted.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file).getDatabase().getEntries();
        assertEquals(List.of(), result);
    }

    @Test
    void importWorksAsExpected() throws URISyntaxException {
        Path file = Path.of(PdfEmbeddedBibFileImporterTest.class.getResource("mixedMetadata.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file).getDatabase().getEntries();

        BibEntry expected = new BibEntry(StandardEntryType.Misc)
                .withCitationKey("jabreftext2021")
                .withField(StandardField.AUTHOR, "Someone embedded")
                .withField(StandardField.TITLE, "I like beds")
                .withField(StandardField.DOI, "10.1002/9781118257517")
                .withField(StandardField.COMMENT, "From embedded bib");

        assertEquals(List.of(expected), result);
    }
}
