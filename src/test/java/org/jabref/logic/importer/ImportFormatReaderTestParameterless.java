package org.jabref.logic.importer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jabref.logic.xmp.XMPPreferences;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImportFormatReaderTestParameterless {

    private ImportFormatReader reader;

    @Before
    public void setUp() {
        reader = new ImportFormatReader();
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getEncoding()).thenReturn(StandardCharsets.UTF_8);
        reader.resetImportFormats(importFormatPreferences, mock(XMPPreferences.class));
    }

    @Test(expected = ImportException.class)
    public void importUnknownFormatThrowsExceptionIfNoMatchingImporterWasFound() throws Exception {
        Path file = Paths.get(ImportFormatReaderTestParameterless.class.getResource("fileformat/emptyFile.xml").toURI());
        reader.importUnknownFormat(file);
        fail();
    }

    @Test(expected = NullPointerException.class)
    public void testNullImportUnknownFormat() throws Exception {
        reader.importUnknownFormat(null);
        fail();
    }

    @Test(expected = ImportException.class)
    public void importFromFileWithUnknownFormatThrowsException() throws Exception {
        reader.importFromFile("someunknownformat", Paths.get("somepath"));
        fail();
    }
}
