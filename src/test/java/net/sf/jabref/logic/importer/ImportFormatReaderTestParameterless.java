package net.sf.jabref.logic.importer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.xmp.XMPPreferences;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ImportFormatReaderTestParameterless {

    private ImportFormatReader reader;

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance(); // Needed for special fields
        reader = new ImportFormatReader();
        reader.resetImportFormats(ImportFormatPreferences.fromPreferences(Globals.prefs),
                XMPPreferences.fromPreferences(Globals.prefs));
    }

    @Test
    public void testImportUnknownFormatNotWorking() throws URISyntaxException {
        Path file = Paths.get(ImportFormatReaderTestParameterless.class.getResource("fileformat/emptyFile.xml").toURI());
        ImportFormatReader.UnknownFormatImport unknownFormat = reader.importUnknownFormat(file);
        assertNull(unknownFormat);
    }

    @Test(expected = NullPointerException.class)
    public void testNullImportUnknownFormat() {
        reader.importUnknownFormat((Path)null);
        fail();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImportFromFileUnknownFormat() throws IOException {
        reader.importFromFile("someunknownformat", Paths.get("somepath"));
        fail();
    }
}
