package org.jabref.logic.importer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

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
        PreferencesService preferencesService = mock(PreferencesService.class, Answers.RETURNS_DEEP_STUBS);
        when(preferencesService.getDefaultEncoding()).thenReturn(StandardCharsets.UTF_8);
        reader.resetImportFormats(preferencesService, mock(XmpPreferences.class), fileMonitor);
    }

    @Test
    void importUnknownFormatThrowsExceptionIfNoMatchingImporterWasFound() throws Exception {
        Path file = Path.of(ImportFormatReaderTestParameterless.class.getResource("fileformat/emptyFile.xml").toURI());
        assertThrows(ImportException.class, () -> reader.importUnknownFormat(file, fileMonitor));
    }

    @Test
    void importUnknownFormatThrowsExceptionIfPathIsNull() throws Exception {
        assertThrows(NullPointerException.class, () -> reader.importUnknownFormat(null, fileMonitor));
    }

    @Test
    void importUnknownFormatThrowsExceptionIfDataIsNull() throws Exception {
        assertThrows(NullPointerException.class, () -> reader.importUnknownFormat(null));
    }

    @Test
    void importFromFileWithUnknownFormatThrowsException() throws Exception {
        assertThrows(ImportException.class, () -> reader.importFromFile("someunknownformat", Path.of("somepath")));
    }
}
