package org.jabref.logic.importer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImportFormatReaderTestParameterless {

    private ImportFormatReader reader;
    private final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();

    @BeforeEach
    public void setUp() {
        reader = new ImportFormatReader();
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getEncoding()).thenReturn(StandardCharsets.UTF_8);
        reader.resetImportFormats(importFormatPreferences, mock(XmpPreferences.class), fileMonitor);
    }

    @Test
    public void importUnknownFormatThrowsExceptionIfNoMatchingImporterWasFound() throws Exception {
        Path file = Paths.get(ImportFormatReaderTestParameterless.class.getResource("fileformat/emptyFile.xml").toURI());
        assertThrows(NullPointerException.class, () -> reader.importUnknownFormat(file, fileMonitor));
    }

    @Test
    public void testNullImportUnknownFormatPath() throws Exception {
        assertThrows(NullPointerException.class, () -> reader.importUnknownFormat(null, fileMonitor));

    }

    @Test
    public void testNullImportUnknownFormatString() throws Exception {
        assertThrows(NullPointerException.class, () -> reader.importUnknownFormat(null));

    }

    @Test
    public void importFromFileWithUnknownFormatThrowsException() throws Exception {
        assertThrows(NullPointerException.class, () -> reader.importFromFile("someunknownformat", Paths.get("somepath")));
    }

}
