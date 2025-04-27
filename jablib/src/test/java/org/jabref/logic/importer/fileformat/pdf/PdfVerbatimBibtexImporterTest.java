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

class PdfVerbatimBibtexImporterTest {

    private PdfVerbatimBibtexImporter importer;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.fieldPreferences().getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());
        importer = new PdfVerbatimBibtexImporter(importFormatPreferences);
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
