package org.jabref.logic.importer;

import java.nio.file.Path;

import javafx.collections.FXCollections;

import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportFormatReaderTestParameterless {

    private ImportFormatReader reader;
    private final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();

    @BeforeEach
    void setUp() {
        reader = new ImportFormatReader();
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importerPreferences.getCustomImportList()).thenReturn(FXCollections.emptyObservableSet());

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        reader.resetImportFormats(importerPreferences, importFormatPreferences, fileMonitor);
    }

    @Test
    void importUnknownFormatThrowsExceptionIfNoMatchingImporterWasFound() throws Exception {
        Path file = Path.of(ImportFormatReaderTestParameterless.class.getResource("fileformat/emptyFile.xml").toURI());
        assertThrows(ImportException.class, () -> reader.importUnknownFormat(file, fileMonitor));
    }

    @Test
    void importUnknownFormatThrowsExceptionIfPathIsNull() {
        assertThrows(NullPointerException.class, () -> reader.importUnknownFormat(null, fileMonitor));
    }

    @Test
    void importUnknownFormatThrowsExceptionIfDataIsNull() {
        assertThrows(NullPointerException.class, () -> reader.importUnknownFormat(null));
    }

    @Test
    void importFromFileWithUnknownFormatThrowsException() {
        assertThrows(ImportException.class, () -> reader.importFromFile("someunknownformat", Path.of("somepath")));
    }
}
