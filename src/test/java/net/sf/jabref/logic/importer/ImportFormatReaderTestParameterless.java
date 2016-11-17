package net.sf.jabref.logic.importer;

import java.nio.file.Path;
import java.nio.file.Paths;

import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

public class ImportFormatReaderTestParameterless {

    private ImportFormatReader reader;

    @Before
    public void setUp() {
        reader = new ImportFormatReader();
        reader.resetImportFormats(JabRefPreferences.getInstance().getImportFormatPreferences(),
                JabRefPreferences.getInstance().getXMPPreferences());
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
