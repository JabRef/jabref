package org.jabref.logic.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EndnoteXmlExporterTest {

    private Exporter exporter;
    private BibDatabaseContext databaseContext;
    private BibEntry bookEntry;

    @BeforeEach
    void setUp() throws Exception {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences()).thenReturn(mock(BibEntryPreferences.class));
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        databaseContext = new BibDatabaseContext();
        exporter = new EndnoteXmlExporter(new BibEntryPreferences(','));

        bookEntry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Bhattacharyya2013")
                .withField(StandardField.EDITOR, "Bhattacharyya, R. and McCormick, M. E.")
                .withField(StandardField.PUBLISHER, "Elsevier Science")
                .withField(StandardField.TITLE, "Wave Energy Conversion")
                .withField(StandardField.YEAR, "2013")
                .withField(StandardField.ISBN, "9780080442129")
                .withField(StandardField.FILE, "/home/mfg/acad/ext/arts/waves/water/[R._Bhattacharyya_and_M.E._McCormick_(Eds.)]_Wave_(z-lib.org).pdf")
                .withField(StandardField.KEYWORDS, "waves, agua");
    }

    @Test
    void exportForEmptyEntryList(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("EmptyFile.xml");

        exporter.export(databaseContext, file, Collections.emptyList());
        assertFalse(Files.exists(file));
    }

    @Test
    void exportForNullDBThrowsException(@TempDir Path tempDir) {
        Path file = tempDir.resolve("NullDB");

        assertThrows(NullPointerException.class, () ->
                exporter.export(null, file, Collections.singletonList(bookEntry)));
    }

    @Test
    void exportForNullExportPathThrowsException(@TempDir Path tempDir) {
        assertThrows(NullPointerException.class, () ->
                exporter.export(databaseContext, null, Collections.singletonList(bookEntry)));
    }

    @Test
    void exportForNullEntryListThrowsException(@TempDir Path tempDir) {
        Path file = tempDir.resolve("EntryNull");

        assertThrows(NullPointerException.class, () ->
                exporter.export(databaseContext, file, null));
    }
}
